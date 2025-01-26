import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Data, NavigationEnd, Router, RouterLink, RouterOutlet, Routes} from '@angular/router';
import {Messages} from './messages';
import messagesDE from "./messages.de.json";
import {NgbCollapseModule, NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {KeyValuePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {Subscription} from "rxjs";
import {Title} from "@angular/platform-browser";
import {getAllLanguagesAndItsNames} from "./app.routes";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NgbCollapseModule, NgbDropdownModule, NgForOf, NgIf, RouterLink, NgClass, KeyValuePipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  isNavbarCollapsed = true;
  routePath = '';
  routeData: Data = []
  messages: Messages = messagesDE;
  routerSub$: Subscription | undefined;

  constructor(private route: ActivatedRoute, private router: Router, private titleService: Title) {
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
          // @ts-ignore
          _paq.push(['setDocumentTitle', this.titleService.getTitle()]);
        }
      }
    })
  }

  ngOnDestroy(): void {
    this.routerSub$?.unsubscribe();
  }

  public trackLanguageChange(newLanguage: string, oldLanguage: string) {
    // @ts-ignore
    _paq.push(['trackEvent', 'Settings', 'languageChange', oldLanguage + '_to_' + newLanguage]);
  }

  public trackNavigation(targetPath: string) {
    AppComponent.trackNavigation(targetPath, this.router.config)
  }

  public static trackNavigation(targetPath: string, routes: Routes) {
    // @ts-ignore
    _paq.push(['setCustomUrl', "/" + targetPath]);
    // @ts-ignore
    _paq.push(['setDocumentTitle', routes.filter(route => route.path === targetPath)[0].title]);
    // @ts-ignore
    _paq.push(['trackPageView']);
  }

  protected readonly allLanguagesAndItsNames = getAllLanguagesAndItsNames();
}
