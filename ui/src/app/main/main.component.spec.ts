import {ComponentFixture, TestBed} from '@angular/core/testing';

import {MainComponent} from './main.component';
import messages from "../messages.en.json";
import {ActivatedRoute} from "@angular/router";
import {of} from "rxjs";

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
    component.events['garden-synodic'] = true;
    const params = component.paramsAsString(true);
    expect(params).toContain('events[garden-synodic]=true');
  });

  it('paramsAsString omits a disabled event flag', () => {
    component.events['garden-biodynamic'] = false;
    const params = component.paramsAsString(true);
    expect(params).not.toContain('events[garden-biodynamic]');
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
