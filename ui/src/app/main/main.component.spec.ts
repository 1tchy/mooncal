import {ComponentFixture, TestBed} from '@angular/core/testing';

import {MainComponent} from './main.component';
import messages from "../messages.en.json";
import messagesDE from "../messages.de.json";
import {ActivatedRoute, Router} from "@angular/router";
import {of} from "rxjs";
import {PLATFORM_ID} from "@angular/core";

describe('MainComponent', () => {
  let component: MainComponent;
  let fixture: ComponentFixture<MainComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [MainComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: route,
          data: of(route.data)
        }
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(MainComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function mockGeolocation(latitude: number, longitude: number) {
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake((success: PositionCallback) =>
      success({coords: {latitude, longitude}} as GeolocationPosition));
  }

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the error message from the server', async () => {
    spyOn(window, 'fetch').and.resolveTo(new Response('When selecting daily phases only 100 years can be displayed.', {status: 400, statusText: 'Bad Request'}));
    component.requestPath = '';
    component.fetchCalendar(false);
    for (let i = 0; i < 50 && !component.error; i++) {
      await new Promise(resolve => setTimeout(resolve, 10));
    }
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('When selecting daily phases only 100 years can be displayed.');
  });

  it('asks the server for error messages in the language of the page', () => {
    const fetchSpy = spyOn(window, 'fetch').and.resolveTo(new Response('[]'));
    component.requestPath = '';
    component.fetchCalendar(false);
    expect(fetchSpy.calls.mostRecent().args[1]).toEqual(jasmine.objectContaining({headers: {'Accept-Language': 'en'}}));
  });

  it(`should have the a field`, () => {
    expect(component.events['lunareclipse']).toEqual(true);
  });

  it('should render title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Moon Calendar');
  });

  it('paramsAsString includes an enabled event flag', () => {
    component.events['garden-biodynamic'] = true;
    const params = component.paramsAsString(true);
    expect(params).toContain('events[garden-biodynamic]=true');
  });

  it('paramsAsString omits a disabled event flag', () => {
    component.events['moonlanding'] = false;
    const params = component.paramsAsString(true);
    expect(params).not.toContain('events[moonlanding]');
  });

  it('paramsAsString omits the location when daily phases are off', () => {
    component.phases['daily'] = false;
    component.lat = 47.4;
    component.lon = 8.5;
    expect(component.paramsAsString(true)).not.toContain('lat=');
    expect(component.paramsAsString(false)).not.toContain('lon=');
  });

  it('paramsAsString includes the location for daily phases', () => {
    component.phases['daily'] = true;
    component.lat = 47.4;
    component.lon = 8.5;
    expect(component.paramsAsString(true)).toContain('&lat=47.4&lon=8.5');
    expect(component.paramsAsString(false)).toContain('&lat=47.4&lon=8.5');
  });

  it('paramsAsString includes the sun flag and the location for sunrise and sunset', () => {
    component.phases['daily'] = false;
    component.phases['sun'] = true;
    component.lat = 47.4;
    component.lon = 8.5;
    const params = component.paramsAsString(false);
    expect(params).toContain('&phases[sun]=true');
    expect(params).toContain('&lat=47.4&lon=8.5');
    expect(component.paramsForTracking(false)).toContain('sun');
  });

  it('tracking never contains the location', () => {
    component.phases['daily'] = true;
    component.lat = 47.4;
    component.lon = 8.5;
    expect(component.paramsForTracking(true)).not.toContain('47.4');
  });

  it('selecting a time zone moves the location to its reference city', () => {
    component.zone = 'America/New_York';
    component.zoneSelected();
    expect(component.lat).toBe(40.7);
    expect(component.lon).toBe(-74);
    expect(component.locationName).toBe('New York');
  });

  it('uses the rounded browser location and the nearest time zone with the same times', () => {
    component.zone = 'Europe/Zurich';
    mockGeolocation(48.39123, -4.48654);
    component.useMyLocation();
    expect(component.lat).toBe(48.4);
    expect(component.lon).toBe(-4.5);
    expect(component.zone).toBe('Europe/Paris');
    expect(component.locationName).toBe('Your location');
  });

  it('keeps a time zone chosen by the user when the location changes', () => {
    component.zone = 'Europe/Zurich';
    component.zoneSelected();
    mockGeolocation(48.39123, -4.48654);
    component.useMyLocation();
    expect(component.lat).toBe(48.4);
    expect(component.lon).toBe(-4.5);
    expect(component.zone).toBe('Europe/Zurich');
  });

  it('keeps the browser location when the user chooses a time zone afterwards', () => {
    mockGeolocation(48.39123, -4.48654);
    component.useMyLocation();
    component.zone = 'America/New_York';
    component.zoneSelected();
    expect(component.lat).toBe(48.4);
    expect(component.lon).toBe(-4.5);
  });

  it('sets the southern hemisphere for a browser location south of the equator', () => {
    component.hemisphere = 'northern';
    mockGeolocation(-33.86785, 151.20732);
    component.useMyLocation();
    expect(component.hemisphere).toBe('southern');
  });

  it('sets the hemisphere for manually entered coordinates', () => {
    component.hemisphere = 'southern';
    component.lat = 47.4;
    component.lon = 8.5;
    component.locationEdited('longitude');
    expect(component.hemisphere).toBe('northern');
    component.lat = -0.1;
    component.locationEdited('latitude');
    expect(component.hemisphere).toBe('southern');
  });

  it('does not change the hemisphere when a time zone is chosen', () => {
    component.hemisphere = 'northern';
    component.zone = 'Australia/Sydney';
    component.zoneSelected();
    expect(component.hemisphere).toBe('northern');
  });

  it('names the nearest reference city, not the one of the time zone', () => {
    component.zone = 'Europe/Zurich';
    component.zoneSelected();
    // the time zone chosen by the user stays Zurich, but the location is in Paris
    component.lat = 48.9;
    component.lon = 2.3;
    component.locationEdited('longitude');
    expect(component.locationName).toBe('Paris');
    expect(component.zone).toBe('Europe/Zurich');
  });

  it('names no city when none is near', () => {
    component.lat = 46.2;
    component.lon = 6.1;
    component.locationEdited('longitude');
    expect(component.locationName).toBeNull();
  });

  it('names the browser location until the coordinates are changed', () => {
    mockGeolocation(47.3769, 8.5417);
    component.useMyLocation();
    expect(component.locationName).toBe('Your location');
    component.lon = 8.6;
    component.locationEdited('longitude');
    expect(component.locationName).toBe('Zurich');
  });

  it('shows an error when the browser location is not available', () => {
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake((_: PositionCallback, error?: PositionErrorCallback | null) =>
      error!({code: 1} as GeolocationPositionError));
    component.useMyLocation();
    expect(component.locationError).toBeTrue();
  });

  it('rounds manually entered coordinates', () => {
    component.lat = 47.3769;
    component.lon = 8.5417;
    component.locationEdited('longitude');
    expect(component.lat).toBe(47.4);
    expect(component.lon).toBe(8.5);
  });

  it('tracks which location field was edited, but not the coordinates', () => {
    // @ts-ignore
    window._paq = [];
    component.lat = 47.3769;
    component.locationEdited('latitude');
    component.lon = 8.5417;
    component.locationEdited('longitude');
    // @ts-ignore
    expect(window._paq).toEqual([
      ['trackEvent', 'Settings', 'locationEdit', 'latitude'],
      ['trackEvent', 'Settings', 'locationEdit', 'longitude'],
    ]);
  });

  it('tracks the use of the browser location, but not the coordinates', () => {
    // @ts-ignore
    window._paq = [];
    mockGeolocation(47.3769, 8.5417);
    component.useMyLocation();
    // @ts-ignore
    expect(window._paq).toEqual([
      ['trackEvent', 'Settings', 'useMyLocation', 'request'],
      ['trackEvent', 'Settings', 'useMyLocation', 'success'],
    ]);
  });

  it('tracks why the browser location is not available', () => {
    // @ts-ignore
    window._paq = [];
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake((_: PositionCallback, error?: PositionErrorCallback | null) =>
      error!({code: 1} as GeolocationPositionError));
    component.useMyLocation();
    // @ts-ignore
    expect(window._paq).toEqual([
      ['trackEvent', 'Settings', 'useMyLocation', 'request'],
      ['trackEvent', 'Settings', 'useMyLocation', 'denied'],
    ]);
  });

  it('shows the location fields only for daily phases', async () => {
    expect(fixture.nativeElement.querySelector('#location')).toBeNull();
    fixture.nativeElement.querySelector('#daily-checkbox').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#location-city').textContent).toContain(component.locationName);
  });

  it('shows the location fields also for sunrise and sunset alone', async () => {
    expect(fixture.nativeElement.querySelector('#location')).toBeNull();
    fixture.nativeElement.querySelector('#sun-checkbox').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.phases['sun']).toBeTrue();
    expect(fixture.nativeElement.querySelector('#location')).not.toBeNull();
    fixture.nativeElement.querySelector('#sun-checkbox').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#location')).toBeNull();
  });

  const STATIONS_FAR_AWAY = {maxDistanceKm: 50, withinMaxDistance: false, nearest: [
    {id: 'ticon/genova-1', label: 'Genova', lat: 44.4, lon: 8.9, km: 334},
    {id: 'ticon/venezia-2', label: 'Venezia', lat: 45.4, lon: 12.4, km: 350},
    {id: 'ticon/marseille-3', label: 'Marseille', lat: 43.3, lon: 5.4, km: 420}]};

  function mockFetch(tideStations: object) {
    return spyOn(window, 'fetch').and.callFake((url: RequestInfo | URL) => Promise.resolve(new Response(
      String(url).startsWith('/tideStations') ? JSON.stringify(tideStations) : '[]')));
  }

  async function until(condition: () => boolean) {
    for (let i = 0; i < 50 && !condition(); i++) {
      await new Promise(resolve => setTimeout(resolve, 10));
    }
  }

  function tickTideAt(lat: number, lon: number) {
    component.lat = lat;
    component.lon = lon;
    fixture.nativeElement.querySelector('#tide-checkbox').click();
  }

  it('sends phases[tide] and the location when tide is ticked', () => {
    component.phases['tide'] = true;
    component.lat = 53.9;
    component.lon = 8.7;
    const params = component.paramsAsString(false);
    expect(params).toContain('&phases[tide]=true');
    expect(params).toContain('&lat=53.9&lon=8.7');
    expect(component.paramsForTracking(false)).toContain('tide');
    expect(component.paramsForTracking(false)).not.toContain('53.9');
  });

  it('shows the 3 nearest stations when none is within the limit', async () => {
    const fetchSpy = mockFetch(STATIONS_FAR_AWAY);
    tickTideAt(47.4, 8.5);
    await until(() => component.tideStationHint !== null);
    fixture.detectChanges();
    expect(fetchSpy).toHaveBeenCalledWith('/tideStations?lat=47.4&lon=8.5');
    const hint = fixture.nativeElement.querySelector('#tide-station-hint');
    expect(hint.textContent).toContain('50 km');
    expect(hint.textContent).toContain('Genova (334 km)');
    expect(hint.querySelectorAll('.tide-station').length).toBe(3);
  });

  it('shows stations of the same name', async () => {
    const warnings = spyOn(console, 'warn').and.callThrough();
    mockFetch({maxDistanceKm: 50, withinMaxDistance: false, nearest: [
      {id: 'ticon/port-1', label: 'Port', lat: 44.4, lon: 8.9, km: 334},
      {id: 'ticon/port-2', label: 'Port', lat: 45.4, lon: 12.4, km: 350}]});
    tickTideAt(47.4, 8.5);
    await until(() => component.tideStationHint !== null);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('#tide-station-hint .tide-station').length).toBe(2);
    expect(warnings.calls.allArgs().map(args => String(args[0])).filter(warning => warning.includes('NG0955'))).toEqual([]);
  });

  it('shows no station hint when a station is near', async () => {
    const fetchSpy = mockFetch({maxDistanceKm: 50, withinMaxDistance: true, nearest: [{id: 'ticon/cuxhaven-1', label: 'Cuxhaven', lat: 53.9, lon: 8.7, km: 3}]});
    tickTideAt(53.9, 8.7);
    await until(() => fetchSpy.calls.count() >= 2);
    await new Promise(resolve => setTimeout(resolve, 20));
    fixture.detectChanges();
    expect(component.tideStationHint).toBeNull();
    expect(fixture.nativeElement.querySelector('#tide-station-hint')).toBeNull();
  });

  it('shows the hint for a station just beyond the limit, though its rounded distance is the limit', async () => {
    mockFetch({maxDistanceKm: 50, withinMaxDistance: false, nearest: [{id: 'ticon/rimini-1', label: 'Rimini', lat: 44.1, lon: 12.6, km: 50}]});
    tickTideAt(43.7, 12.6);
    await until(() => component.tideStationHint !== null);
    expect(component.tideStationHint).not.toBeNull();
  });

  it('fetches the stations when the location changes, but not when the calendar is updated', async () => {
    const fetchSpy = mockFetch(STATIONS_FAR_AWAY);
    tickTideAt(47.4, 8.5);
    await until(() => component.tideStationHint !== null);
    const stationRequests = () => fetchSpy.calls.allArgs().map(args => String(args[0])).filter(url => url.startsWith('/tideStations'));
    expect(stationRequests()).toEqual(['/tideStations?lat=47.4&lon=8.5']);
    const fetchHintSpy = spyOn<any>(component, 'fetchTideStationHint').and.callThrough();
    component.style = 'fullmoon';
    component.fetchCalendar(false);
    expect(fetchHintSpy).not.toHaveBeenCalled();
    expect(stationRequests().length).toBe(1);
    component.lat = 46.2;
    component.locationEdited('latitude');
    expect(stationRequests()).toEqual(['/tideStations?lat=47.4&lon=8.5', '/tideStations?lat=46.2&lon=8.5']);
  });

  it('keeps the station hint of the new location when the request of the old one fails later', async () => {
    let failOldRequest: (reason: unknown) => void = () => {};
    spyOn(window, 'fetch').and.callFake((url: RequestInfo | URL) => String(url) === '/tideStations?lat=47.4&lon=8.5'
      ? new Promise<Response>((_, reject) => failOldRequest = reject)
      : Promise.resolve(new Response(String(url).startsWith('/tideStations') ? JSON.stringify(STATIONS_FAR_AWAY) : '[]')));
    tickTideAt(47.4, 8.5);
    component.lat = 46.2;
    component.locationEdited('latitude');
    await until(() => component.tideStationHint !== null);
    failOldRequest(new Error('offline'));
    await new Promise(resolve => setTimeout(resolve, 20));
    expect(component.tideStationHint).not.toBeNull();
  });

  it('asks again for the stations of a location whose request failed', async () => {
    let failed = false;
    spyOn(window, 'fetch').and.callFake((url: RequestInfo | URL) => {
      if (String(url).startsWith('/tideStations') && !failed) {
        failed = true;
        return Promise.reject(new Error('offline'));
      }
      return Promise.resolve(new Response(String(url).startsWith('/tideStations') ? JSON.stringify(STATIONS_FAR_AWAY) : '[]'));
    });
    tickTideAt(47.4, 8.5);
    await new Promise(resolve => setTimeout(resolve, 20));
    component.locationEdited('latitude');
    await until(() => component.tideStationHint !== null);
    expect(component.tideStationHint).not.toBeNull();
  });

  it('hides the station hint when tide is unticked', async () => {
    mockFetch(STATIONS_FAR_AWAY);
    tickTideAt(47.4, 8.5);
    await until(() => component.tideStationHint !== null);
    fixture.nativeElement.querySelector('#tide-checkbox').click();
    expect(component.tideStationHint).toBeNull();
  });

  async function chooseFirstStation() {
    const fetchSpy = mockFetch(STATIONS_FAR_AWAY);
    tickTideAt(47.4, 8.5);
    await until(() => component.tideStationHint !== null);
    fixture.detectChanges();
    // @ts-ignore
    window._paq = [];
    fixture.nativeElement.querySelector('.tide-station').click();
    fixture.detectChanges();
    return fetchSpy;
  }

  function lastCalendarRequest(fetchSpy: jasmine.Spy) {
    return fetchSpy.calls.allArgs().map(args => String(args[0])).filter(url => url.startsWith('/mooncal?')).pop()!;
  }

  it('shows the tides of a clicked station without moving the location', async () => {
    const fetchSpy = await chooseFirstStation();
    expect(component.lat).toBe(47.4);
    expect(component.lon).toBe(8.5);
    const request = lastCalendarRequest(fetchSpy);
    // nothing else needs the location
    expect(request).not.toContain('&lat=');
    expect(request).toContain('&tideStation=ticon%2Fgenova-1');
    const chosen = fixture.nativeElement.querySelector('.tide-station');
    expect(chosen.getAttribute('aria-pressed')).toBe('true');
    expect(chosen.textContent).toContain('✓');
    expect(fixture.nativeElement.querySelectorAll('.tide-station[aria-pressed="true"]').length).toBe(1);
    // @ts-ignore
    expect(window._paq).toEqual([['trackEvent', 'Settings', 'tideStationSelected']]);
    expect(component.paramsForTracking(true)).not.toContain('genova');
  });

  it('shows no tides when the chosen station is clicked again', async () => {
    const fetchSpy = await chooseFirstStation();
    fixture.nativeElement.querySelector('.tide-station').click();
    fixture.detectChanges();
    expect(lastCalendarRequest(fetchSpy)).not.toContain('tideStation');
    expect(lastCalendarRequest(fetchSpy)).toContain('&lat=47.4&lon=8.5');
    expect(fixture.nativeElement.querySelectorAll('.tide-station[aria-pressed="true"]').length).toBe(0);
  });

  it('sends the location with a chosen station when the sunrise needs it', async () => {
    await chooseFirstStation();
    component.phases['sun'] = true;
    const params = component.paramsAsString(true);
    expect(params).toContain('&lat=47.4&lon=8.5');
    expect(params).toContain('&tideStation=ticon%2Fgenova-1');
  });

  it('forgets the chosen station when the location changes', async () => {
    const fetchSpy = await chooseFirstStation();
    component.lat = 46.2;
    component.locationEdited('latitude');
    expect(lastCalendarRequest(fetchSpy)).not.toContain('tideStation');
  });

  it('forgets the chosen station when the time zone moves the location', async () => {
    await chooseFirstStation();
    component.zone = 'Europe/Rome';
    component.zoneSelected();
    expect(component.tideStation).toBeNull();
  });

  it('keeps the chosen station while tides are unticked, but does not send it', async () => {
    const fetchSpy = await chooseFirstStation();
    fixture.nativeElement.querySelector('#tide-checkbox').click();
    expect(component.paramsAsString(true)).not.toContain('tideStation');
    fixture.nativeElement.querySelector('#tide-checkbox').click();
    expect(component.paramsAsString(true)).toContain('&tideStation=ticon%2Fgenova-1');
  });

  it('shows the year hint outside 1900-2100', async () => {
    component.phases['tide'] = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#tide-years-hint')).toBeNull();
    component.to = '2105-12-31';
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#tide-years-hint').textContent).toContain('1900');
  });

  it('shows no year hint from the first to the last day of 1900-2100 in any time zone', async () => {
    // a date alone would be read as midnight UTC, which is still the year before west of UTC
    component.phases['tide'] = true;
    component.from = '1900-01-01';
    component.to = '2100-12-31';
    await fixture.whenStable();
    fixture.detectChanges();
    expect(component.tideOutsideYears).toBeFalse();
  });

  it('home page shows moon landing and hides the garden checkbox', () => {
    expect(component.isGarden).toBeFalse();
    expect(component.visibleEvents).toEqual(['lunareclipse', 'solareclipse', 'moonlanding']);
    expect(component.events['garden-biodynamic']).toBeFalse();
  });

  it('garden page shows the garden checkbox (on by default) and hides moon landing', () => {
    component.routeData = {...component.routeData, id: 'garden'};
    expect(component.isGarden).toBeTrue();
    expect(component.visibleEvents).toEqual(['lunareclipse', 'solareclipse', 'garden-biodynamic']);
    expect(component.events['garden-biodynamic']).toBeTrue();
    expect(component.events['moonlanding']).toBeFalse();
    expect(component.heading).toEqual(messages.garden.title);
    expect(component.introduction).toEqual(messages.garden.introduction);
  });

  it('formats a single-day event as one date (no range)', () => {
    const text = component.formatEventDateForGui({date: '2026-01-03T10:00', title: 't', description: 'd'});
    expect(text).not.toContain(' - ');
    expect(text).toEqual(component.formatDateForGui('2026-01-03T10:00'));
  });

  it('formats a multi-day event as a from - to range', () => {
    const event = {date: '2026-01-03T08:30', endDate: '2026-01-05T22:00', title: 't', description: 'd'};
    const text = component.formatEventDateForGui(event);
    expect(text).toContain(' - ');
    expect(text).toEqual(component.formatDateTimeForGui('2026-01-03T08:30') + ' - ' + component.formatDateTimeForGui('2026-01-05T22:00'));
  });
});

describe('MainComponent rendered on the server', () => {
  let component: MainComponent;
  let fixture: ComponentFixture<MainComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [MainComponent],
      providers: [
        {provide: PLATFORM_ID, useValue: 'server'},
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: route,
            data: of(route.data)
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MainComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not fetch the calendar during server-side rendering', () => {
    const fetchSpy = spyOn(window, 'fetch').and.resolveTo(new Response('[]'));
    component.fetchCalendar();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('keeps the default subscription tab instead of reading the user agent', () => {
    expect(component.initialSubscriptionDescriptionOS).toBe(component.SUBSCRIPTION_DESCRIPTION_IOS);
  });
});

describe('MainComponent on a German page with an English-speaking browser', () => {
  let component: MainComponent;
  let router: Router;

  const clearRedirectCookie = () => document.cookie = 'redirectedFromGerman=; max-age=0';
  const redirectIfUserNotUnderstands = () => (component as any).redirectIfUserNotUnderstands(['en-US', 'en'], router);

  beforeEach(async () => {
    const route = {data: {messages: messagesDE}};
    await TestBed.configureTestingModule({
      imports: [MainComponent],
      providers: [{provide: ActivatedRoute, useValue: {snapshot: route, data: of(route.data)}}]
    }).compileComponents();
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    clearRedirectCookie();
    component = TestBed.createComponent(MainComponent).componentInstance;
    (router.navigate as jasmine.Spy).calls.reset();
    clearRedirectCookie();
  });

  afterEach(clearRedirectCookie);

  it('redirects to English when the German page is the landing page', () => {
    redirectIfUserNotUnderstands();
    expect(router.navigate).toHaveBeenCalledWith(['/en']);
  });

  it('does not redirect when the user navigated to German within the app', () => {
    router.navigated = true;
    redirectIfUserNotUnderstands();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
