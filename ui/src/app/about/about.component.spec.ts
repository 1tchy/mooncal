import {ComponentFixture, TestBed} from '@angular/core/testing';
import {AboutComponent} from './about.component';
import messages from "../messages.en.json";
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {HttpClient} from "@angular/common/http";

describe('AboutComponent', () => {
  let component: AboutComponent;
  let fixture: ComponentFixture<AboutComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [AboutComponent],
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

    fixture = TestBed.createComponent(AboutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
