import {Component} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Messages} from "../messages";

@Component({
  selector: 'app-support-buttons',
  standalone: true,
  imports: [],
  templateUrl: './support-buttons.component.html',
  styleUrl: './support-buttons.component.css'
})
export class SupportButtonsComponent {

  messages: Messages;

  constructor(route: ActivatedRoute) {
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }
}
