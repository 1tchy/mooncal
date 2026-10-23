import {AfterViewInit, ChangeDetectorRef, Component, inject, PLATFORM_ID, TemplateRef, ViewChild} from '@angular/core';
import {Messages} from '../messages';
import {Event} from "./event";
import {isPlatformBrowser, NgClass} from "@angular/common";
import {FormsModule, NgForm} from "@angular/forms";
import {ActivatedRoute, Data, Router, RouterLink, Routes} from "@angular/router";
import {debounceTime, distinctUntilChanged, Subject} from "rxjs";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap/modal";
import {NgbNavChangeEvent, NgbNavModule} from "@ng-bootstrap/ng-bootstrap";

import {getAllLanguages} from "../app.routes";
import {ButtonWithStyledTooltip} from "../button-with-styled-tooltip/button-with-styled-tooltip.component";
import {MainSettingsService} from "./main-settings.service";
import {ZONE_LOCATIONS} from "./zone-locations";
import {isValidLocation, nearCity, nearestZoneWithSameTimes, referenceLocation, roundLocation} from "./location";

// Time frame of subscriptions around today
const SUBSCRIPTION_MONTHS_BEFORE = 6;
const SUBSCRIPTION_YEARS_AFTER = 2;

// GeolocationPositionError codes, for tracking why the browser location is not available
const GEOLOCATION_ERRORS: Record<number, string> = {1: 'denied', 2: 'unavailable', 3: 'timeout'};

@Component({
  selector: 'app-main',
  standalone: true,
  imports: [
    FormsModule,
    NgClass,
    NgbNavModule,
    RouterLink,
    ButtonWithStyledTooltip
  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent implements AfterViewInit {
  private modalService = inject(NgbModal);
  private changeDetectorRef = inject(ChangeDetectorRef);
  private settings = inject(MainSettingsService);
  // During prerendering there is no real browser: skip everything that needs window/navigator/cookies.
  private isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  messages: Messages;
  routeData: Data = []
  routes: Routes;

  @ViewChild('optionsForm') private optionsForm!: NgForm;
  get phases() { return this.settings.phases; }
  set phases(v) { this.settings.phases = v; }
  get style() { return this.settings.style; }
  set style(v) { this.settings.style = v; }
  get hemisphere() { return this.settings.hemisphere; }
  set hemisphere(v) { this.settings.hemisphere = v; }
  get events() { return this.isGarden ? this.settings.gardenEvents : this.settings.events; }
  set events(v) { if (this.isGarden) { this.settings.gardenEvents = v; } else { this.settings.events = v; } }

  // The Garden-calendar page reuses this component; it is told apart by its route id.
  get isGarden() { return this.routeData['id'] === 'garden'; }
  get heading() { return this.isGarden ? this.messages.garden.title : this.messages.app.title; }
  get introduction() { return this.isGarden ? this.messages.garden.introduction : this.messages.app.introduction; }
  get visibleEvents() {
    return this.isGarden
      ? ['lunareclipse', 'solareclipse', 'garden-biodynamic']
      : ['lunareclipse', 'solareclipse', 'moonlanding'];
  }
  get from() { return this.settings.from; }
  set from(v) { this.settings.from = v; }
  get to() { return this.settings.to; }
  set to(v) { this.settings.to = v; }
  get zone() { return this.settings.zone; }
  set zone(v) { this.settings.zone = v; }
  get lat() { return this.settings.lat; }
  set lat(v) { this.settings.lat = v; }
  get lon() { return this.settings.lon; }
  set lon(v) { this.settings.lon = v; }
  /** "Your location" for the browser's location, else the reference city near the location, if any. */
  get locationName() {
    const browserLocation = this.settings.browserLocation;
    if (browserLocation && browserLocation.lat === this.lat && browserLocation.lon === this.lon) {
      return this.messages.phases.location.yourLocation;
    }
    return isValidLocation(this.lat, this.lon) ? nearCity(this.lat!, this.lon!) : null;
  }
  locationError = false;
  /** Moonrise/-set of the daily phases and sunrise/-set are calculated for the location. */
  get usesLocation() { return this.phases['daily'] || this.phases['sun']; }

  from$ = new Subject<Date>();
  fromDebounced = this.toDate(this.from, true);
  to$ = new Subject<Date>();
  toDebounced = this.toDate(this.to, false);
  oldZone = this.zone;

  created = Date.now() - 1704067200000; // - 2024-01-01
  requestPath = "";
  calendar: Event[] = [];
  requestOngoing = false;
  error: any = null;
  readonly SUBSCRIPTION_DESCRIPTION_IOS = 1;
  readonly SUBSCRIPTION_DESCRIPTION_MAC = 2;
  readonly SUBSCRIPTION_DESCRIPTION_GOOGLE = 3;
  readonly SUBSCRIPTION_DESCRIPTION_ANDROID = 4;
  readonly SUBSCRIPTION_DESCRIPTION_THUNDERBIRD = 5;
  readonly SUBSCRIPTION_DESCRIPTION_OUTLOOK = 6;
  readonly SUBSCRIPTION_DESCRIPTION_MAX = this.SUBSCRIPTION_DESCRIPTION_OUTLOOK;
  initialSubscriptionDescriptionOS = this.SUBSCRIPTION_DESCRIPTION_IOS;
  activeSubscriptionDescriptionOS = this.initialSubscriptionDescriptionOS;
  clipboardSet$ = new Subject<boolean>();
  clipboardSet = false;
  trackIcalSubscriptionTextarea$ = new Subject<string>();

  constructor(route: ActivatedRoute, router: Router) {
    this.routeData = route.snapshot.data;
    this.routes = router.config;
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.routeData = data;
      this.messages = data['messages']
      if (this.style == "withDescription" && this.messages.styles.fullmoonAndName == "") {
        this.style = "fullmoon";
      }
    })
    if (this.style == "withDescription" && this.messages.styles.fullmoonAndName == "") {
      this.style = "fullmoon";
    }
    this.from$.pipe(debounceTime(500), distinctUntilChanged()).subscribe((date: Date) => {
      this.fromDebounced = date;
      this.fetchCalendar();
    })
    this.to$.pipe(debounceTime(500), distinctUntilChanged()).subscribe((date: Date) => {
      this.toDebounced = date;
      this.fetchCalendar();
    })
    this.clipboardSet$.pipe(debounceTime(2000)).subscribe(() => {
      this.clipboardSet = false;
    })
    this.trackIcalSubscriptionTextarea$.pipe(distinctUntilChanged()).subscribe(() => {
      this.doTrackIcalSubscriptionTextarea();
    })
    this.initialSubscriptionDescriptionOS = this.guessInitialSubscriptionDescriptionOS();
    this.activeSubscriptionDescriptionOS = this.initialSubscriptionDescriptionOS;
    if (this.isBrowser) {
      this.redirectIfUserNotUnderstands(navigator.languages, router);
    }
  }

  private redirectIfUserNotUnderstands(usersLanguages: ReadonlyArray<string>, router: Router) {
    if (this.messages.lang.current !== 'de') {
      return;
    }
    if (router.navigated) {
      // Only redirect on the landing page, not when the user chose German within the app
      return;
    }
    if (document.cookie.indexOf('redirectedFromGerman') >= 0) {
      return;
    }
    if (usersLanguages.find(value => value.startsWith('de')) !== undefined) {
      return;
    }
    for (const userLanguage of usersLanguages) {
      for (const supportedLanguage of getAllLanguages()) {
        if (supportedLanguage === this.messages.lang.current) {
          continue;
        }
        if (userLanguage.startsWith(supportedLanguage)) {
          document.cookie = "redirectedFromGerman=true;max-age=3600";
          // noinspection JSIgnoredPromiseFromCall
          router.navigate(['/' + supportedLanguage]);
          return;
        }
      }
    }
  }

  ngAfterViewInit() {
    this.optionsForm.form.valueChanges.subscribe(() => {
      setTimeout(() => {//timeout so that Angular can update the components fields
        this.fetchCalendar(false);
      }, 1)
    });
  }

  public toDate(date: string, startOfDay: boolean) {
    return new Date(date + (startOfDay ? "T00:00:00" : "T23:59:59"))
  }

  public getApiUrl(withHost: boolean, suffix: string | null) {
    let path = "/mooncal" + (suffix ? "." + suffix : "");
    if (withHost) {
      return window.location.protocol + "//" + window.location.host + path
    } else {
      return path
    }
  }

  public paramsAsString(useFromTo: boolean) {
    // Boolean flags are only emitted when enabled; an omitted flag is read as false by the backend.
    let options = "lang=" + this.messages.lang.current
      + this.flagParam("phases[full]", this.phases["full"])
      + this.flagParam("phases[new]", this.phases["new"])
      + this.flagParam("phases[quarter]", this.phases["quarter"])
      + this.flagParam("phases[daily]", this.phases["daily"])
      + this.flagParam("phases[sun]", this.phases["sun"])
      + (this.usesLocation && isValidLocation(this.lat, this.lon) ? "&lat=" + this.lat + "&lon=" + this.lon : "")
      + "&style=" + this.style
      + "&hemisphere=" + this.hemisphere
      + this.flagParam("events[lunareclipse]", this.events["lunareclipse"])
      + this.flagParam("events[solareclipse]", this.events["solareclipse"])
      + this.flagParam("events[moonlanding]", this.events["moonlanding"])
      + this.flagParam("events[garden-biodynamic]", this.events["garden-biodynamic"]);
    if (useFromTo) {
      options += "&from=" + this.formatDate(this.fromDebounced) + "&to=" + this.formatDate(this.toDebounced);
    } else {
      options += "&before=P" + SUBSCRIPTION_MONTHS_BEFORE + "M&after=P" + SUBSCRIPTION_YEARS_AFTER + "Y&zone=" + this.zone;
    }
    return options;
  }

  private flagParam(key: string, value: boolean) {
    return value ? "&" + key + "=true" : "";
  }

  public paramsForTracking(useFromTo: boolean) {
    let params = (this.phases["full"] ? "full," : "")
      + (this.phases["new"] ? "new," : "")
      + (this.phases["quarter"] ? "quarter," : "")
      + (this.phases["daily"] ? "daily," : "")
      + (this.phases["sun"] ? "sun," : "")
      + this.style + ","
      + (this.hemisphere === "southern" ? "southern," : "")
      + (this.events["lunareclipse"] ? "lunareclipse," : "")
      + (this.events["solareclipse"] ? "solareclipse," : "")
      + (this.events["moonlanding"] ? "moonlanding" : "");
    if (params.endsWith(",")) {
      params = params.substring(0, params.length - 1);
    }
    if (useFromTo) {
      if (MainSettingsService.initialFrom() === this.formatDateOnly(this.fromDebounced)) {
        params += ",initial";
      } else {
        params += "," + this.formatDateOnly(this.fromDebounced);
      }
      if (MainSettingsService.initialTo() === this.formatDateOnly(this.toDebounced)) {
        params += "-initial";
      } else {
        params += "-" + this.formatDateOnly(this.toDebounced);
      }
    }
    return params;
  }

  // xs uses a 2-digit year to save horizontal space on narrow screens.
  public formatDateForGui(date: any, xs = false) {
    return new Date(date).toLocaleDateString(undefined, {year: xs ? '2-digit' : 'numeric', month: '2-digit', day: '2-digit'});
  }

  public formatDateTimeForGui(local: any, xs = false) {
    return new Date(local).toLocaleString(undefined, {year: xs ? '2-digit' : 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'});
  }

  public formatEventDateForGui(event: Event, xs = false) {
    return event.endDate
      ? this.formatDateTimeForGui(event.date, xs) + ' - ' + this.formatDateTimeForGui(event.endDate, xs)
      : this.formatDateForGui(event.date, xs);
  }

  public formatDate(date: Date) {
    if (!date) return date;
    return date.getFullYear()
      + '-' + this.pad(date.getMonth() + 1)
      + '-' + this.pad(date.getDate())
      + 'T' + this.pad(date.getHours())
      + ':' + this.pad(date.getMinutes())
      + ':' + this.pad(date.getSeconds())
      + this.zone;
  }

  private formatDateOnly(date: Date) {
    if (!date) return date;
    return date.getFullYear()
      + '-' + this.pad(date.getMonth() + 1)
      + '-' + this.pad(date.getDate());
  }

  private pad(num: number) {
    const norm = Math.abs(Math.floor(num));
    return (norm < 10 ? '0' : '') + norm;
  }

  public fetchCalendar(track: boolean = true) {
    if (!this.isBrowser) {
      return;
    }
    let requestPath = this.paramsAsString(true);
    if (!Number.isNaN(this.fromDebounced.getTime()) && !Number.isNaN(this.toDebounced.getTime())) {
      if (this.requestPath !== requestPath) {
        this.requestPath = requestPath;
        this.requestOngoing = true;
        let url = this.getApiUrl(false, null) + "?" + requestPath;
        fetch(url)
          .then(response => {
            if (!response.ok) {
              throw response;
            }
            return response.json();
          })
          .then((data: any) => {
            this.error = null;
            this.updateCalendar(data);
            this.requestOngoing = false;
          })
          .catch((response) => {
            this.error = response;
            this.calendar = [];
            this.requestOngoing = false;
          });
        if (track) {
          // @ts-ignore
          _paq.push(['trackEvent', 'Calendar', 'update', this.paramsForTracking(true)]);
        }
      }
    } else {
      this.calendar = [];
    }
  }

  public updateCalendar(newCalendar: Event[]) {
    this.calendar = newCalendar;
    this.changeDetectorRef.markForCheck();
  }

  public subscriptionDescriptionOSChanged(event: NgbNavChangeEvent) {
    this.doTrackSubscriptionDescriptionOSChanged(this.getSubscriptionDescriptionOSName(event.activeId), this.getSubscriptionDescriptionOSName(event.nextId));
  }

  public copyIcalLink() {
    // noinspection JSIgnoredPromiseFromCall
    navigator.clipboard.writeText(document.getElementById('icalLink')!.textContent!);
    this.clipboardSet = true;
    this.clipboardSet$.next(this.clipboardSet);
    this.trackIcalSubscriptionCopyClipboardButton();
  }

  public trackDownloadIcal() {
    // @ts-ignore
    _paq.push(['trackLink', 'https://mooncal.ch/download/' + this.paramsForTracking(true), 'download']);
  }

  public trackOpenSubscriptionModal() {
    // @ts-ignore
    _paq.push(['trackEvent', 'Calendar', 'openSubscriptionModal', this.paramsForTracking(false)]);
  }

  private doTrackSubscriptionDescriptionOSChanged(oldOS: string, newOS: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'subscriptionDescriptionOSChange', oldOS + "_to_" + newOS]);
  }

  private trackIcalSubscriptionCopyClipboardButton() {
    // @ts-ignore
    _paq.push(['trackLink', 'https://mooncal.ch/subscribeCopyClipboardButton/' + this.paramsForTracking(false), 'download']);
  }

  public trackIcalSubscriptionTextarea() {
    this.trackIcalSubscriptionTextarea$.next(document.getElementById('icalLink')!.textContent!)
  }

  private doTrackIcalSubscriptionTextarea() {
    // @ts-ignore
    _paq.push(['trackLink', 'https://mooncal.ch/subscribeTextField/' + this.paramsForTracking(false), 'download']);
  }

  public trackPdfDownload() {
    // @ts-ignore
    _paq.push(['trackLink', 'https://mooncal.ch/pdf/' + this.paramsForTracking(true), 'download']);
  }

  public trackTimezoneChange() {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'timezoneChange', this.oldZone + "_to_" + this.zone]);
    this.oldZone = this.zone;
  }

  //see SupportedTimeZoneTest
  readonly supportedTimezones = Object.keys(ZONE_LOCATIONS);

  /** A manually chosen time zone moves the location to the zone's reference city, unless it's the browser's location. */
  public zoneSelected() {
    this.settings.zoneChosenByUser = true;
    if (!this.settings.browserLocation) {
      ({lat: this.lat, lon: this.lon} = referenceLocation(this.zone));
    }
    this.trackTimezoneChange();
    this.fetchCalendar(false);
  }

  public locationEdited(field: 'latitude' | 'longitude') {
    this.locationError = false;
    this.lat = roundLocation(this.lat);
    this.lon = roundLocation(this.lon);
    this.trackLocationEdit(field);
    this.locationProvided();
  }

  public useMyLocation() {
    this.locationError = false;
    this.trackUseMyLocation('request');
    navigator.geolocation.getCurrentPosition(position => {
      // rounded right away, the precise position is never used or sent anywhere
      const browserLocation = {lat: roundLocation(position.coords.latitude), lon: roundLocation(position.coords.longitude)};
      this.settings.browserLocation = browserLocation;
      ({lat: this.lat, lon: this.lon} = browserLocation);
      this.trackUseMyLocation('success');
      this.locationProvided();
      this.changeDetectorRef.markForCheck();
    }, error => {
      this.trackUseMyLocation(GEOLOCATION_ERRORS[error.code] ?? 'error');
      this.locationError = true;
      this.changeDetectorRef.markForCheck();
    }, {enableHighAccuracy: false, maximumAge: 3600000, timeout: 30000});
  }

  // The coordinates are never tracked, only which field was edited.
  private trackLocationEdit(field: 'latitude' | 'longitude') {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'locationEdit', field]);
  }

  // The coordinates are never tracked, only whether the browser provided them.
  private trackUseMyLocation(outcome: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'useMyLocation', outcome]);
  }

  private locationProvided() {
    if (isValidLocation(this.lat, this.lon)) {
      this.hemisphere = this.lat! < 0 ? 'southern' : 'northern';
    }
    this.adoptNearestZone();
    this.fetchCalendar(false);
  }

  /**
   * Shows the time zone of the nearest reference city, without changing any of the displayed times.
   * Only as long as the user did not choose the time zone.
   */
  private adoptNearestZone() {
    if (this.settings.zoneChosenByUser || !isValidLocation(this.lat, this.lon) || Number.isNaN(this.fromDebounced.getTime()) || Number.isNaN(this.toDebounced.getTime())) {
      return;
    }
    const today = new Date();
    // also cover the time frame of subscriptions
    const from = new Date(Math.min(this.fromDebounced.getTime(), new Date(today.getFullYear(), today.getMonth() - SUBSCRIPTION_MONTHS_BEFORE, today.getDate()).getTime()));
    const to = new Date(Math.max(this.toDebounced.getTime(), new Date(today.getFullYear() + SUBSCRIPTION_YEARS_AFTER, today.getMonth(), today.getDate() + 1).getTime()));
    this.zone = nearestZoneWithSameTimes(this.zone, this.lat!, this.lon!, from, to);
    this.oldZone = this.zone;
  }

  private guessInitialSubscriptionDescriptionOS() {
    if (!this.isBrowser) {
      return this.SUBSCRIPTION_DESCRIPTION_IOS;
    }
    const agent = window.navigator.userAgent.toLowerCase()
    switch (true) {
      case agent.indexOf('iphone') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_IOS
      case agent.indexOf('android') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_ANDROID
      case agent.indexOf('safari') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_MAC
      case agent.indexOf('firefox') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_THUNDERBIRD
      case agent.indexOf('chrome') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_GOOGLE
      case agent.indexOf('edge') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_OUTLOOK
      case agent.indexOf('trident') > -1:
        return this.SUBSCRIPTION_DESCRIPTION_OUTLOOK
      default:
        return Math.random() * this.SUBSCRIPTION_DESCRIPTION_MAX;
    }
  }

  private getSubscriptionDescriptionOSName(subscriptionDescriptionOSId: number) {
    switch (subscriptionDescriptionOSId) {
      case this.SUBSCRIPTION_DESCRIPTION_IOS:
        return "iOS"
      case this.SUBSCRIPTION_DESCRIPTION_ANDROID:
        return "Android"
      case this.SUBSCRIPTION_DESCRIPTION_MAC:
        return "Mac"
      case this.SUBSCRIPTION_DESCRIPTION_GOOGLE:
        return "Google"
      case this.SUBSCRIPTION_DESCRIPTION_THUNDERBIRD:
        return "Thunderbird"
      case this.SUBSCRIPTION_DESCRIPTION_OUTLOOK:
        return "Outlook"
      default:
        return "Unknown"
    }
  }

  subscribeOpen(content: TemplateRef<any>) {
    this.modalService.open(content, {ariaLabelledBy: 'subscribeModalLabel', size: 'lg'})
  }
}
