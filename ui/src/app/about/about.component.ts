import {Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";
import {FormsModule} from "@angular/forms";
import {SupportButtonsComponent} from "../support-buttons/support-buttons.component";
import {ImproveTranslationComponent} from "../improve-translation/improve-translation.component";

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [
    FormsModule,
    SupportButtonsComponent,
    ImproveTranslationComponent
  ],
  templateUrl: './about.component.html',
  styleUrl: './about.component.css'
})
export class AboutComponent {

  messages: Messages;

  constructor(route: ActivatedRoute) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }
}
