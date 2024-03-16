import {Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [],
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

  public trackSupport(supportType: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Support', supportType]);
  }
}
