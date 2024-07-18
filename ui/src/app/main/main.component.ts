import {AfterViewInit, Component, inject, TemplateRef, ViewChild} from '@angular/core';
import {Messages} from '../messages';
import {Event} from "./event";
import {DatePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {FormsModule, NgForm} from "@angular/forms";
import {ActivatedRoute, Router} from "@angular/router";
import {debounceTime, distinctUntilChanged, Subject} from "rxjs";
import {
  NgbDropdownModule,
  NgbModal,
  NgbNav,
  NgbNavContent,
  NgbNavItem,
  NgbNavLinkButton,
  NgbNavOutlet
} from "@ng-bootstrap/ng-bootstrap";
import {HttpClient} from "@angular/common/http";
import {getAllLanguages} from "../app.routes";

type options = { [key: string]: boolean }

@Component({
  selector: 'app-main',
  standalone: true,
  imports: [
    NgIf,
    NgForOf,
    FormsModule,
    NgClass,
    NgbDropdownModule,
    NgbNav, NgbNavItem, NgbNavLinkButton, NgbNavContent, NgbNavOutlet,
    DatePipe
  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.css'
})
export class MainComponent implements AfterViewInit {
  private modalService = inject(NgbModal);
  messages: Messages;

  @ViewChild('optionsForm') private optionsForm!: NgForm;
  phases: options = {
    full: true,
    'new': false,
    quarter: false,
    daily: false
  };
  events: options = {lunareclipse: true, solareclipse: true, moonlanding: true};

  from = MainComponent.initialFrom();
  from$ = new Subject<Date>();
  fromDebounced = this.toDate(this.from);
  to = MainComponent.initialTo();
  to$ = new Subject<Date>();
  toDebounced = this.toDate(this.to);
  zone = this.getTimezone();

  updateCount = 0;
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
  activeSubscriptionDescriptionOS = this.SUBSCRIPTION_DESCRIPTION_IOS;

  constructor(route: ActivatedRoute, private router: Router, private httpClient: HttpClient) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
    this.from$.pipe(debounceTime(500), distinctUntilChanged()).subscribe((date: Date) => {
      this.fromDebounced = date;
      this.fetchCalendar();
    })
    this.to$.pipe(debounceTime(500), distinctUntilChanged()).subscribe((date: Date) => {
      this.toDebounced = date;
      this.fetchCalendar();
    })
    this.activeSubscriptionDescriptionOS = this.guessInitialSubscriptionDescriptionOS()
    this.redirectIfUserNotUnderstands(navigator.languages, router);
  }

  private static initialFrom() {
    return new Date().getFullYear() + "-01-01";
  }

  private static initialTo() {
    return new Date().getFullYear() + "-12-31";
  }

  private redirectIfUserNotUnderstands(usersLanguages: ReadonlyArray<string>, router: Router) {
    if (this.messages.lang.current !== 'de') {
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
        this.fetchCalendar();
      }, 1)
    });
  }

  public toDate(date: string) {
    return new Date(date)
  }

  public getApiUrl(withHost: boolean, ics: boolean) {
    let path = "/mooncal" + (ics ? ".ics" : "");
    if (withHost) {
      return window.location.protocol + "//" + window.location.host + path
    } else {
      return path
    }
  }

  public paramsAsString(useFromTo: boolean) {
    let options = "lang=" + this.messages.lang.current
      + "&phases[full]=" + this.phases["full"]
      + "&phases[new]=" + this.phases["new"]
      + "&phases[quarter]=" + this.phases["quarter"]
      + "&phases[daily]=" + this.phases["daily"]
      + "&events[lunareclipse]=" + this.events["lunareclipse"]
      + "&events[solareclipse]=" + this.events["solareclipse"]
      + "&events[moonlanding]=" + this.events["moonlanding"];
    if (useFromTo) {
      options += "&from=" + this.formatDate(this.fromDebounced) + "&to=" + this.formatDate(this.toDebounced);
    } else {
      options += "&before=P6M&after=P2Y&zone=" + this.zone;
    }
    return options;
  }

  public paramsForTracking(useFromTo: boolean) {
    let params = (this.phases["full"] ? "full," : "")
      + (this.phases["new"] ? "new," : "")
      + (this.phases["quarter"] ? "quarter," : "")
      + (this.phases["daily"] ? "daily," : "")
      + (this.events["lunareclipse"] ? "lunareclipse," : "")
      + (this.events["solareclipse"] ? "solareclipse," : "")
      + (this.events["moonlanding"] ? "moonlanding" : "");
    if (params.endsWith(",")) {
      params = params.substring(0, params.length - 1);
    }
    if (useFromTo) {
      if (MainComponent.initialFrom() === this.formatDateOnly(this.fromDebounced)) {
        params += ",initial";
      } else {
        params += "," + this.formatDateOnly(this.fromDebounced);
      }
      if (MainComponent.initialTo() === this.formatDateOnly(this.toDebounced)) {
        params += "-initial";
      } else {
        params += "-" + this.formatDateOnly(this.toDebounced);
      }
    }
    return params;
  }

  public formatDateForGui(date: any) {
    return new Date(date).toLocaleDateString(undefined, {year: 'numeric', month: '2-digit', day: '2-digit'});
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

  public fetchCalendar() {
    let requestPath = this.paramsAsString(true);
    if (!Number.isNaN(this.fromDebounced.getTime()) && !Number.isNaN(this.toDebounced.getTime())) {
      if (this.requestPath !== requestPath) {
        this.requestPath = requestPath;
        this.requestOngoing = true;
        let url = this.getApiUrl(false, false) + "?" + requestPath;
        this.httpClient.get(url).subscribe({
          next: (data: any) => {
            this.error = null;
            this.updateCalendar(data);
            this.requestOngoing = false;
          },
          error: (response) => {
            this.error = response;
            this.calendar = [];
            this.requestOngoing = false;
          }
        });
        if (this.updateCount++ > 0) {
          // @ts-ignore
          _paq.push(['trackEvent', 'Calendar', 'update', this.paramsForTracking(true)]);
        }
      }
    } else {
      this.calendar = [];
    }
  }

  public updateCalendar(newCalendar: Event[]) {
    if (!this.calendar) {
      this.calendar = newCalendar;
      return;
    }
    let newBase = 0;
    for (let o = 0; o < this.calendar.length;) {
      const oldElement = this.calendar[o];
      if (this.eventsEquals(oldElement, newCalendar[newBase])) {
        o++;
        newBase++; //nothing to do, event already in list
      } else {
        let oldElementInNewList = false;
        for (let n = newBase; n < newCalendar.length; n++) {
          if (this.eventsEquals(oldElement, newCalendar[n])) {
            oldElementInNewList = true;
            break;
          }
        }
        if (oldElementInNewList) {
          this.calendar.splice(o++, 0, newCalendar[newBase++]); //add new event
        } else {
          this.calendar.splice(o, 1); //delete old event
        }
      }
    }
    while (newBase < newCalendar.length) {
      this.calendar.push(newCalendar[newBase++]);
    }
  }

  public eventsEquals(event1: Event, event2: Event) {
    return event1 && event2 && event1.date === event2.date && event1.title === event2.title && event1.description === event2.description;
  }

  public copyIcalLink() {
    // noinspection JSIgnoredPromiseFromCall
    navigator.clipboard.writeText(document.getElementById('icalLink')!.textContent!);
  }

  public trackDownloadIcal() {
    // @ts-ignore
    _paq.push(['trackLink', this.paramsForTracking(true), 'download']);
  }

  public trackIcalSubscription() {
    // @ts-ignore
    _paq.push(['trackEvent', 'Calendar', 'subscribeIcal', this.paramsForTracking(false)]);
  }

  public trackPrint() {
    // @ts-ignore
    _paq.push(['trackEvent', 'Calendar', 'print', this.paramsForTracking(true)]);
  }

  public trackTimezoneChange() {
    // @ts-ignore
    _paq.push(['trackEvent', 'Calendar', 'timezone', this.zone]);
  }

  public getTimezone() {
    return Intl.DateTimeFormat().resolvedOptions().timeZone
  }

  public getSupportedTimezones() {
    //see SupportedTimeZoneTest
    return ["Etc/GMT+12", "Pacific/Midway", "Pacific/Niue", "Pacific/Pago_Pago", "Pacific/Samoa", "US/Samoa", "Pacific/Honolulu", "Pacific/Johnston", "Pacific/Rarotonga", "Pacific/Tahiti", "US/Hawaii", "America/Adak", "America/Atka", "Pacific/Marquesas", "US/Aleutian", "Pacific/Gambier", "America/Anchorage", "America/Juneau", "America/Metlakatla", "America/Nome", "America/Sitka", "America/Yakutat", "US/Alaska", "Pacific/Pitcairn", "America/Dawson", "America/Ensenada", "America/Los_Angeles", "America/Santa_Isabel", "America/Tijuana", "America/Vancouver", "America/Whitehorse", "Canada/Pacific", "Canada/Yukon", "Mexico/BajaNorte", "PST8PDT", "US/Pacific", "America/Creston", "America/Dawson_Creek", "America/Fort_Nelson", "America/Hermosillo", "America/Phoenix", "US/Arizona", "America/Boise", "America/Cambridge_Bay", "America/Chihuahua", "America/Ciudad_Juarez", "America/Denver", "America/Edmonton", "America/Inuvik", "America/Mazatlan", "America/Ojinaga", "America/Shiprock", "America/Yellowknife", "Canada/Mountain", "MST7MDT", "Mexico/BajaSur", "Navajo", "US/Mountain", "America/Belize", "America/Costa_Rica", "America/El_Salvador", "America/Guatemala", "America/Managua", "America/Regina", "America/Swift_Current", "America/Tegucigalpa", "Canada/Saskatchewan", "Pacific/Galapagos", "America/Bahia_Banderas", "America/Chicago", "America/Indiana/Knox", "America/Indiana/Tell_City", "America/Knox_IN", "America/Matamoros", "America/Menominee", "America/Merida", "America/Mexico_City", "America/Monterrey", "America/North_Dakota/Beulah", "America/North_Dakota/Center", "America/North_Dakota/New_Salem", "America/Rainy_River", "America/Rankin_Inlet", "America/Resolute", "America/Winnipeg", "CST6CDT", "Canada/Central", "Mexico/General", "US/Central", "US/Indiana-Starke", "America/Atikokan", "America/Bogota", "America/Cancun", "America/Cayman", "America/Coral_Harbour", "America/Eirunepe", "America/Guayaquil", "America/Jamaica", "America/Lima", "America/Panama", "America/Porto_Acre", "America/Rio_Branco", "Brazil/Acre", "Chile/EasterIsland", "Jamaica", "Pacific/Easter", "America/Caracas", "America/Detroit", "America/Fort_Wayne", "America/Havana", "America/Indiana/Indianapolis", "America/Indiana/Marengo", "America/Indiana/Petersburg", "America/Indiana/Vevay", "America/Indiana/Vincennes", "America/Indiana/Winamac", "America/Indianapolis", "America/Iqaluit", "America/Kentucky/Louisville", "America/Kentucky/Monticello", "America/Louisville", "America/Montreal", "America/Nassau", "America/New_York", "America/Nipigon", "America/Pangnirtung", "America/Port-au-Prince", "America/Thunder_Bay", "America/Toronto", "Canada/Eastern", "Cuba", "EST5EDT", "US/East-Indiana", "US/Eastern", "US/Michigan", "America/Anguilla", "America/Antigua", "America/Aruba", "America/Barbados", "America/Blanc-Sablon", "America/Boa_Vista", "America/Curacao", "America/Dominica", "America/Grand_Turk", "America/Grenada", "America/Guadeloupe", "America/Guyana", "America/Kralendijk", "America/La_Paz", "America/Lower_Princes", "America/Manaus", "America/Marigot", "America/Martinique", "America/Montserrat", "America/Port_of_Spain", "America/Porto_Velho", "America/Puerto_Rico", "America/Santo_Domingo", "America/St_Barthelemy", "America/St_Kitts", "America/St_Lucia", "America/St_Thomas", "America/St_Vincent", "America/Tortola", "America/Virgin", "Brazil/West", "America/Asuncion", "America/Campo_Grande", "America/Cuiaba", "America/Glace_Bay", "America/Goose_Bay", "America/Halifax", "America/Moncton", "America/Thule", "Atlantic/Bermuda", "Canada/Atlantic", "America/Araguaina", "America/Argentina/Buenos_Aires", "America/Argentina/Catamarca", "America/Argentina/ComodRivadavia", "America/Argentina/Cordoba", "America/Argentina/Jujuy", "America/Argentina/La_Rioja", "America/Argentina/Mendoza", "America/Argentina/Rio_Gallegos", "America/Argentina/Salta", "America/Argentina/San_Juan", "America/Argentina/San_Luis", "America/Argentina/Tucuman", "America/Argentina/Ushuaia", "America/Bahia", "America/Belem", "America/Buenos_Aires", "America/Catamarca", "America/Cayenne", "America/Cordoba", "America/Fortaleza", "America/Jujuy", "America/Maceio", "America/Mendoza", "America/Montevideo", "America/Paramaribo", "America/Punta_Arenas", "America/Recife", "America/Rosario", "America/Santarem", "America/Santiago", "America/St_Johns", "Antarctica/Palmer", "Antarctica/Rothera", "Atlantic/Stanley", "Canada/Newfoundland", "Chile/Continental", "America/Godthab", "America/Miquelon", "America/Nuuk", "America/Sao_Paulo", "Brazil/East", "America/Noronha", "Atlantic/South_Georgia", "Brazil/DeNoronha", "Atlantic/Cape_Verde", "America/Scoresbysund", "Atlantic/Azores", "Africa/Abidjan", "Africa/Accra", "Africa/Bamako", "Africa/Banjul", "Africa/Bissau", "Africa/Casablanca", "Africa/Conakry", "Africa/Dakar", "Africa/El_Aaiun", "Africa/Freetown", "Africa/Lome", "Africa/Monrovia", "Africa/Nouakchott", "Africa/Ouagadougou", "Africa/Sao_Tome", "Africa/Timbuktu", "America/Danmarkshavn", "Atlantic/Reykjavik", "Atlantic/St_Helena", "GMT", "GMT0", "Greenwich", "Iceland", "UCT", "UTC", "Universal", "Zulu", "Atlantic/Canary", "Atlantic/Faeroe", "Atlantic/Faroe", "Atlantic/Madeira", "Eire", "Europe/Belfast", "Europe/Dublin", "Europe/Guernsey", "Europe/Isle_of_Man", "Europe/Jersey", "Europe/Lisbon", "Europe/London", "GB", "GB-Eire", "Portugal", "WET", "Africa/Algiers", "Africa/Bangui", "Africa/Brazzaville", "Africa/Douala", "Africa/Kinshasa", "Africa/Lagos", "Africa/Libreville", "Africa/Luanda", "Africa/Malabo", "Africa/Ndjamena", "Africa/Niamey", "Africa/Porto-Novo", "Africa/Tunis", "Antarctica/Troll", "Africa/Ceuta", "Africa/Windhoek", "Arctic/Longyearbyen", "Atlantic/Jan_Mayen", "CET", "Europe/Amsterdam", "Europe/Andorra", "Europe/Belgrade", "Europe/Berlin", "Europe/Bratislava", "Europe/Brussels", "Europe/Budapest", "Europe/Busingen", "Europe/Copenhagen", "Europe/Gibraltar", "Europe/Ljubljana", "Europe/Luxembourg", "Europe/Madrid", "Europe/Malta", "Europe/Monaco", "Europe/Oslo", "Europe/Paris", "Europe/Podgorica", "Europe/Prague", "Europe/Rome", "Europe/San_Marino", "Europe/Sarajevo", "Europe/Skopje", "Europe/Stockholm", "Europe/Tirane", "Europe/Vaduz", "Europe/Vatican", "Europe/Vienna", "Europe/Warsaw", "Europe/Zagreb", "Europe/Zurich", "MET", "Poland", "Africa/Blantyre", "Africa/Bujumbura", "Africa/Cairo", "Africa/Gaborone", "Africa/Harare", "Africa/Johannesburg", "Africa/Kigali", "Africa/Lubumbashi", "Africa/Lusaka", "Africa/Maputo", "Africa/Maseru", "Africa/Mbabane", "Africa/Tripoli", "Egypt", "Europe/Kaliningrad", "Libya", "Asia/Amman", "Asia/Beirut", "Asia/Damascus", "Asia/Famagusta", "Asia/Gaza", "Asia/Hebron", "Asia/Istanbul", "Asia/Jerusalem", "Asia/Nicosia", "Asia/Tel_Aviv", "EET", "Europe/Athens", "Europe/Bucharest", "Europe/Chisinau", "Europe/Helsinki", "Europe/Istanbul", "Europe/Kiev", "Europe/Kyiv", "Europe/Mariehamn", "Europe/Nicosia", "Europe/Riga", "Europe/Sofia", "Europe/Tallinn", "Europe/Tiraspol", "Europe/Uzhgorod", "Europe/Vilnius", "Europe/Zaporozhye", "Israel", "Turkey", "Africa/Addis_Ababa", "Africa/Asmara", "Africa/Asmera", "Africa/Dar_es_Salaam", "Africa/Djibouti", "Africa/Juba", "Africa/Kampala", "Africa/Khartoum", "Africa/Mogadishu", "Africa/Nairobi", "Antarctica/Syowa", "Asia/Aden", "Asia/Baghdad", "Asia/Bahrain", "Asia/Kuwait", "Asia/Qatar", "Asia/Riyadh", "Europe/Astrakhan", "Europe/Kirov", "Europe/Minsk", "Europe/Moscow", "Europe/Saratov", "Europe/Simferopol", "Europe/Ulyanovsk", "Europe/Volgograd", "Indian/Antananarivo", "Indian/Comoro", "Indian/Mayotte", "W-SU", "Asia/Dubai", "Asia/Muscat", "Asia/Tbilisi", "Asia/Tehran", "Asia/Yerevan", "Europe/Samara", "Indian/Mahe", "Indian/Mauritius", "Indian/Reunion", "Iran", "Asia/Baku", "Asia/Kabul", "Antarctica/Mawson", "Asia/Aqtau", "Asia/Aqtobe", "Asia/Ashgabat", "Asia/Ashkhabad", "Asia/Atyrau", "Asia/Dushanbe", "Asia/Karachi", "Asia/Oral", "Asia/Samarkand", "Asia/Tashkent", "Asia/Yekaterinburg", "Indian/Kerguelen", "Indian/Maldives", "Asia/Calcutta", "Asia/Colombo", "Asia/Kolkata", "Asia/Kathmandu", "Asia/Katmandu", "Antarctica/Vostok", "Asia/Almaty", "Asia/Barnaul", "Asia/Bishkek", "Asia/Dacca", "Asia/Dhaka", "Asia/Kashgar", "Asia/Novosibirsk", "Asia/Omsk", "Asia/Qostanay", "Asia/Qyzylorda", "Asia/Thimbu", "Asia/Thimphu", "Asia/Tomsk", "Asia/Urumqi", "Indian/Chagos", "Asia/Rangoon", "Asia/Yangon", "Indian/Cocos", "Antarctica/Davis", "Asia/Bangkok", "Asia/Ho_Chi_Minh", "Asia/Jakarta", "Asia/Krasnoyarsk", "Asia/Novokuznetsk", "Asia/Phnom_Penh", "Asia/Pontianak", "Asia/Saigon", "Asia/Vientiane", "Indian/Christmas", "Asia/Hovd", "Antarctica/Casey", "Asia/Brunei", "Asia/Chita", "Asia/Chongqing", "Asia/Chungking", "Asia/Harbin", "Asia/Hong_Kong", "Asia/Irkutsk", "Asia/Kuala_Lumpur", "Asia/Kuching", "Asia/Macao", "Asia/Macau", "Asia/Makassar", "Asia/Manila", "Asia/Shanghai", "Asia/Singapore", "Asia/Taipei", "Asia/Ujung_Pandang", "Australia/Perth", "Australia/West", "Hongkong", "PRC", "Singapore", "Asia/Choibalsan", "Asia/Ulaanbaatar", "Asia/Ulan_Bator", "Asia/Pyongyang", "Australia/Eucla", "Asia/Dili", "Asia/Jayapura", "Asia/Khandyga", "Asia/Seoul", "Asia/Tokyo", "Asia/Yakutsk", "Japan", "Pacific/Palau", "ROK", "Australia/Darwin", "Australia/North", "Antarctica/DumontDUrville", "Asia/Magadan", "Asia/Sakhalin", "Asia/Ust-Nera", "Asia/Vladivostok", "Australia/Adelaide", "Australia/Brisbane", "Australia/Broken_Hill", "Australia/Lindeman", "Australia/Queensland", "Australia/South", "Australia/Yancowinna", "Pacific/Chuuk", "Pacific/Guam", "Pacific/Port_Moresby", "Pacific/Saipan", "Pacific/Truk", "Pacific/Yap", "Antarctica/Macquarie", "Australia/ACT", "Australia/Canberra", "Australia/Currie", "Australia/Hobart", "Australia/Melbourne", "Australia/NSW", "Australia/Sydney", "Australia/Tasmania", "Australia/Victoria", "Australia/LHI", "Australia/Lord_Howe", "Asia/Srednekolymsk", "Pacific/Bougainville", "Pacific/Efate", "Pacific/Guadalcanal", "Pacific/Kosrae", "Pacific/Noumea", "Pacific/Pohnpei", "Pacific/Ponape", "Pacific/Norfolk", "Asia/Anadyr", "Asia/Kamchatka", "Kwajalein", "Pacific/Funafuti", "Pacific/Kwajalein", "Pacific/Majuro", "Pacific/Nauru", "Pacific/Tarawa", "Pacific/Wake", "Pacific/Wallis", "Antarctica/McMurdo", "Antarctica/South_Pole", "NZ", "Pacific/Auckland", "Pacific/Fiji", "Pacific/Enderbury", "Pacific/Fakaofo", "Pacific/Kanton", "Pacific/Tongatapu", "NZ-CHAT", "Pacific/Chatham", "Pacific/Apia", "Pacific/Kiritimati"]
  }

  private guessInitialSubscriptionDescriptionOS() {
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

  subscribeOpen(content: TemplateRef<any>) {
    this.modalService.open(content, {ariaLabelledBy: 'subscribeModalLabel', size: 'lg'})
  }
}
