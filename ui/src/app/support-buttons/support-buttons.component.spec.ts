import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupportButtonsComponent } from './support-buttons.component';
import {ActivatedRoute} from "@angular/router";
import {of} from "rxjs";
import messages from "../messages.en.json";

describe('SupportComponent', () => {
  let component: SupportButtonsComponent;
  let fixture: ComponentFixture<SupportButtonsComponent>;

  beforeEach(async () => {
    const route = {data: {messages: messages}};
    await TestBed.configureTestingModule({
      imports: [SupportButtonsComponent],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          snapshot: route,
          data: of(route.data)
        }
      }]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SupportButtonsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
