import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AppComponent} from './app.component';
import messages from "./messages.en.json";
import {ActivatedRoute} from "@angular/router";
import {of} from "rxjs";

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
