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
