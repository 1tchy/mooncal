import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupportButtonsComponent } from './support-buttons.component';

describe('SupportComponent', () => {
  let component: SupportButtonsComponent;
  let fixture: ComponentFixture<SupportButtonsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupportButtonsComponent]
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
