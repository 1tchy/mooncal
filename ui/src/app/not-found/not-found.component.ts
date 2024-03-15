import {Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Messages} from "../messages";

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [],
  templateUrl: './not-found.component.html',
  styleUrl: './not-found.component.scss'
})
export class NotFoundComponent {
  messages: Messages;

  constructor(route: ActivatedRoute) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }
}
