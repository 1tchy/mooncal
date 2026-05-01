import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router, RouterLink, RouterOutlet} from '@angular/router';
import {Messages} from './messages';
import messagesDE from "./messages.de.json";
import {NgbCollapseModule, NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {DOCUMENT, KeyValuePipe, NgClass} from "@angular/common";
import {Meta} from "@angular/platform-browser";
import {Subscription} from "rxjs";
import {getAllLanguages, getAllLanguagesAndItsNames} from "./app.routes";
import {AB} from "./ab";

const BASE_URL = 'https://mooncal.ch/';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NgbCollapseModule, NgbDropdownModule, RouterLink, NgClass, KeyValuePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  isNavbarCollapsed = true;
  routePath = '';
  routeData: Data = []
  messages: Messages = messagesDE;
  routerSub$: Subscription | undefined;

  constructor(private route: ActivatedRoute, private router: Router, private meta: Meta, @Inject(DOCUMENT) private document: Document, ab: AB) {
    // @ts-ignore
    _paq.push(['setCustomDimension', 1, ab.isA ? 'A' : 'B']);
  }

  ngOnInit() {
    // from https://github.com/angular/angular/issues/11812#issuecomment-346520626
    this.routerSub$ = this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        let r = this.route;
        while (r.firstChild) r = r.firstChild
        if (r.outlet === 'primary') {
          this.routePath = r.snapshot.routeConfig?.path!
          this.routeData = r.snapshot.data
          this.messages = r.snapshot.data['messages']
          const description = r.snapshot.data['description'];
          if (description) {
            this.meta.updateTag({name: 'description', content: description});
          }
          this.document.documentElement.lang = this.messages.lang.current;
          if (this.routePath !== '**') {
            this.updateCanonicalAndHreflangs();
          }
          // @ts-ignore
          _paq.push(['setCustomUrl', "/" + this.routePath + window.location.search]);
          // @ts-ignore
          _paq.push(['setDocumentTitle', this.routeData['id'] + window.location.search]);
          /* tracker methods like "setCustomDimension" should be called before "trackPageView" */
          // @ts-ignore
          _paq.push(['trackPageView']);
        }
      }
    })
  }

  private updateCanonicalAndHreflangs() {
    const canonicalPath = this.routeData['id'] === 'buymeacoffee' ? this.routeData['thank'] : this.routePath;
    let canonical = this.document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!canonical) {
      canonical = this.document.createElement('link');
      canonical.setAttribute('rel', 'canonical');
      this.document.head.appendChild(canonical);
    }
    canonical.setAttribute('href', BASE_URL + canonicalPath);

    this.document.querySelectorAll('link[rel="alternate"][hreflang]').forEach(el => el.remove());
    for (const lang of getAllLanguages()) {
      const path = this.routeData[lang];
      if (path !== undefined) {
        const link = this.document.createElement('link');
        link.setAttribute('rel', 'alternate');
        link.setAttribute('hreflang', lang);
        link.setAttribute('href', BASE_URL + path);
        this.document.head.appendChild(link);
      }
    }
    const xDefault = this.document.createElement('link');
    xDefault.setAttribute('rel', 'alternate');
    xDefault.setAttribute('hreflang', 'x-default');
    xDefault.setAttribute('href', BASE_URL + this.routeData['de']);
    this.document.head.appendChild(xDefault);
  }

  ngOnDestroy(): void {
    this.routerSub$?.unsubscribe();
  }

  public trackLanguageChange(newLanguage: string, oldLanguage: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'languageChange', oldLanguage + '_to_' + newLanguage]);
  }

  protected readonly allLanguagesAndItsNames = getAllLanguagesAndItsNames();
}
