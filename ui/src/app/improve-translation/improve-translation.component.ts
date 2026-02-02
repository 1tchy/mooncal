import {Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";
import {FormsModule} from "@angular/forms";


@Component({
  selector: 'app-improve-translation',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './improve-translation.component.html',
  styleUrl: './improve-translation.component.css'
})
export class ImproveTranslationComponent {

  messages: Messages;
  oldText = '';
  betterText = '';
  suggestBetterTranslationInProgress = false;
  suggestBetterTranslationResult = '';

  constructor(route: ActivatedRoute) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }

  public suggestBetterTranslation() {
    this.suggestBetterTranslationResult = ''
    this.suggestBetterTranslationInProgress = true
    const form = new FormData;
    form.append('language', this.messages.lang.current);
    form.append('oldText', this.oldText);
    form.append('betterText', this.betterText);
    return fetch(`/suggestBetterTranslation`, {
      method: 'POST',
      body: form
    }).then(() => {
      this.suggestBetterTranslationResult = this.messages.improveTranslation.thanksForFeedback
      this.oldText = '';
      this.betterText = '';
      this.suggestBetterTranslationInProgress = false
    });
  }
}
