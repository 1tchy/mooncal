import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Messages} from '../messages';

@Component({
  selector: 'app-suggest-calendar',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './suggest-calendar.component.html',
  styleUrl: './suggest-calendar.component.css'
})
export class SuggestCalendarComponent {
  messages: Messages;
  title = '';
  methodology = '';
  lunarVariables: string[] = [];
  source = '';
  sourceLicense = '';
  sourceLicenseOther = '';
  originCulture = '';
  hemisphereDependence = '';
  outputGranularity = '';
  contactEmail = '';
  notes = '';
  website = ''; // honeypot
  inProgress = false;
  result = '';

  constructor(route: ActivatedRoute) {
    this.messages = route.snapshot.data['messages'];
    route.data.subscribe(data => this.messages = data['messages']);
  }

  toggleVariable(v: string, checked: boolean) {
    this.lunarVariables = checked
      ? [...this.lunarVariables, v]
      : this.lunarVariables.filter(x => x !== v);
  }

  submit() {
    this.inProgress = true;
    const form = new FormData();
    form.append('title', this.title);
    form.append('methodology', this.methodology);
    this.lunarVariables.forEach((v, i) => form.append(`lunarVariables[${i}]`, v));
    form.append('source', this.source);
    form.append('sourceLicense', this.sourceLicense);
    form.append('sourceLicenseOther', this.sourceLicenseOther);
    form.append('originCulture', this.originCulture);
    form.append('hemisphereDependence', this.hemisphereDependence);
    form.append('outputGranularity', this.outputGranularity);
    form.append('contactEmail', this.contactEmail);
    form.append('notes', this.notes);
    form.append('website', this.website);
    return fetch('/suggestCalendar', {method: 'POST', body: form}).then(() => {
      this.result = this.messages.suggestCalendar.thanks;
      this.inProgress = false;
    });
  }
}
