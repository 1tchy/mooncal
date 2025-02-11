import {Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";
import {FormsModule} from "@angular/forms";
import {SupportButtonsComponent} from "../support-buttons/support-buttons.component";
import {ImproveTranslationComponent} from "../improve-translation/improve-translation.component";
import {AB} from "../ab";
import {NgIf} from "@angular/common";

@Component({
  selector: 'app-thank',
  standalone: true,
  imports: [
    FormsModule,
    SupportButtonsComponent,
    ImproveTranslationComponent,
    NgIf
  ],
  templateUrl: './thank.component.html',
  styleUrl: './thank.component.css'
})
export class ThankComponent {

  messages: Messages;

  constructor(route: ActivatedRoute, public ab: AB) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }
}
