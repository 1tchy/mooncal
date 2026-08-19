import {ComponentFixture, TestBed} from '@angular/core/testing';

import {MainComponent} from './main.component';
import messages from "../messages.en.json";
import {ActivatedRoute} from "@angular/router";
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

  it('should create', () => {
    expect(component).toBeTruthy();
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
