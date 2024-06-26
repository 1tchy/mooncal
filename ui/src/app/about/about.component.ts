import {AfterViewInit, Component} from '@angular/core';
import {Messages} from "../messages";
import {ActivatedRoute} from "@angular/router";
import {FormsModule} from "@angular/forms";
import {HttpClient} from "@angular/common/http";

@Component({
  selector: 'app-about',
  standalone: true,
  imports: [
    FormsModule
  ],
  templateUrl: './about.component.html',
  styleUrl: './about.component.css'
})
export class AboutComponent implements AfterViewInit {

  private readonly route: ActivatedRoute;
  messages: Messages;
  oldText = '';
  betterText = '';
  suggestBetterTranslationInProgress = false;
  suggestBetterTranslationResult = '';

  constructor(route: ActivatedRoute, private http: HttpClient) {
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

  public suggestBetterTranslation() {
    this.suggestBetterTranslationResult = ''
    this.suggestBetterTranslationInProgress = true
    const form = new FormData;
    form.append('language', this.messages.lang.current);
    form.append('oldText', this.oldText);
    form.append('betterText', this.betterText);
    return this.http.post(`/suggestBetterTranslation`, form).subscribe(() => {
      this.suggestBetterTranslationResult = this.messages.about.improveTranslation.thanksForFeedback
      this.oldText = '';
      this.betterText = '';
      this.suggestBetterTranslationInProgress = false
    });
  }
}
