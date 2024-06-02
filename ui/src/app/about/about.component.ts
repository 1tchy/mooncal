import {AfterViewInit, Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [],
  templateUrl: './about.component.html',
  styleUrl: './about.component.css'
})
export class AboutComponent implements AfterViewInit {

  private readonly route: ActivatedRoute;
  messages: Messages;

  constructor(route: ActivatedRoute) {
    this.route = route;
    this.messages = route.snapshot.data['messages']
    route.data.subscribe(data => {
      this.messages = data['messages']
    })
  }

  ngAfterViewInit(): void {
    this.redirectOnBuymeacoffeeSite();
  }

  private redirectOnBuymeacoffeeSite() {
    let url = this.route.snapshot.url;
    if (url && "buymeacoffee" === url[url.length - 1].path && !this.canGoForward()) {
      window.setTimeout(() => {
        window.location.href = document.getElementById("buymeacoffee")!.getAttribute("href")!;
      }, 1);
    }
  }

  private canGoForward(): boolean {
    if ('navigation' in window && 'canGoForward' in (window as any).navigation) {
      return (window as any).navigation.canGoForward;
    }
    return false;
  }

  public trackSupport(supportType: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Support', supportType]);
  }
}
