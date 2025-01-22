import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ImproveTranslationComponent} from './improve-translation.component';
import messages from "../messages.en.json";
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {HttpClient} from "@angular/common/http";

describe('ImproveTranslationComponent', () => {
  let component: ImproveTranslationComponent;
  let fixture: ComponentFixture<ImproveTranslationComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [ImproveTranslationComponent],
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

    fixture = TestBed.createComponent(ImproveTranslationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
