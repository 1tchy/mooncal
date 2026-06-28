import {ComponentFixture, TestBed} from '@angular/core/testing';
import {SuggestCalendarComponent} from './suggest-calendar.component';
import messages from "../messages.en.json";
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";

describe('SuggestCalendarComponent', () => {
  let component: SuggestCalendarComponent;
  let fixture: ComponentFixture<SuggestCalendarComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [SuggestCalendarComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: route,
          data: of(route.data)
        }
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(SuggestCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain(messages.suggestCalendar.title);
  });

  it('should call fetch with /suggestCalendar on submit', async () => {
    const fetchSpy = spyOn(window, 'fetch').and.returnValue(Promise.resolve(new Response()));
    component.title = 'Test Calendar';
    component.methodology = 'Test methodology';
    component.source = 'Test source';
    await component.submit();
    expect(fetchSpy).toHaveBeenCalledWith('/suggestCalendar', jasmine.objectContaining({method: 'POST'}));
  });
});
