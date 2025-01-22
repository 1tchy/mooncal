import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ThankComponent} from './thank.component';
import messages from "../messages.en.json";
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {HttpClient} from "@angular/common/http";

describe('ThankComponent', () => {
  let component: ThankComponent;
  let fixture: ComponentFixture<ThankComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [ThankComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: route,
          data: of(route.data)
        }
      }, {
        provide: HttpClient,
        useValue: {}
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(ThankComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
