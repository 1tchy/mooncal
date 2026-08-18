import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AppComponent, sanitizeSearchForTracking} from './app.component';
import messages from "./messages.en.json";
import {ActivatedRoute} from "@angular/router";
import {of} from "rxjs";

// @ts-ignore
window._paq = [];

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: route,
          data: of(route.data)
        }
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the app', () => {
    expect(component).toBeTruthy();
  });
});

describe('sanitizeSearchForTracking', () => {
  it('replaces the fbclid value with a constant', () => {
    expect(sanitizeSearchForTracking('?fbclid=IwcGRvZgVmZGlkFlDGRKtG4YL_7b3bu7XNMHZJVFhwBTVleHRuA2FlbQIxMQBzcnRjBmFwcF9pZAo2NjI4NTY4Mzc5AAEeRmaiqpjbK_1R0hUM-e0k'))
      .toBe('?fbclid=redacted');
  });

  it('keeps other parameters untouched', () => {
    expect(sanitizeSearchForTracking('?lang=de&fbclid=IwAR123abc&phases=true'))
      .toBe('?lang=de&fbclid=redacted&phases=true');
  });

  it('leaves a search without fbclid unchanged', () => {
    expect(sanitizeSearchForTracking('?lang=de&phases=true'))
      .toBe('?lang=de&phases=true');
  });

  it('leaves an empty search unchanged', () => {
    expect(sanitizeSearchForTracking('')).toBe('');
  });
});
