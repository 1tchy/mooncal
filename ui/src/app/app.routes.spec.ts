import {getAllLanguages, routes} from './app.routes';
import {compiledRoute} from './app.routes.compiled.spec';
import {Route} from "@angular/router";
import {Messages} from "./messages";

describe('Routes', () => {
  it('not found route data should equal to german home', () => {
    expect(getRoute('**').data).toEqual(getRoute('').data);
  });
  it('all languages have all pages', () => {
    for (const language of getAllLanguages()) {
      expect(getRouteByLang(language)).withContext(language).toBeDefined();
      expect(getRouteByLang(language, "about")).withContext(language + " - about").toBeDefined();
    }
    expect(getRoute('**').data).toEqual(getRoute('').data);
  });
  it('all pages link to their main and about page', () => {
    for (const route of routes) {
      let language = getMessages(route).lang.current;
      expect(route.data!['home']).withContext(language).toEqual(getRouteByLang(language).path);
      expect(route.data!['about']).withContext(language).toEqual(getRouteByLang(language, 'about').path);
    }
  });
  it('all pages link to all other languages', () => {
    for (const route of routes) {
      let language = getMessages(route).lang.current;
      let page = route.path!.replace(language, '').replace('/', '').replace('**', '');
      for (const otherLanguage of getAllLanguages()) {
        if (otherLanguage === language) {
          continue;
        }
        expect(route.data![otherLanguage]).withContext(otherLanguage + ' in ' + language).toEqual(getRouteByLang(otherLanguage, page).path);
      }
    }
  });
  it('all titles are translated', () => {
    let allTitles = routes.filter(r => !r.path!.endsWith('buymeacoffee')).filter(r => r.title).map(r => r.title!);
    let uniqueTitles = new Set(allTitles);
    expect(uniqueTitles.size).toEqual(allTitles.length);
  });
  it('all components have all languages', () => {
    for (const component of new Set(routes.filter(r => r.path !== '**').map(r => r.component!))) {
      for (const language of getAllLanguages()) {
        expect(routes.filter(r => !r.path!.endsWith('buymeacoffee')).filter(r => r.component === component && getMessages(r).lang.current === language).length).withContext(component + ' in ' + language).toBe(1);
      }
    }
  });
  it('all routes do not accidentally change', () => {
    const routesFormatted = routes.map(r => {
      return {
        path: r.path,
        title: r.title,
        component: r.component!.name,
        data: {
          messages: r.data!['messages'].lang.current,
          home: r.data!['home'],
          about: r.data!['about'],
        }
      }
    });
    expect(routesFormatted).toEqual(compiledRoute);
  });

  function getRouteByLang(language: string, urlPostfix: string = ''): Route {
    let url: string;
    if (language === 'de') {
      url = urlPostfix;
    } else if (urlPostfix === '') {
      url = language;
    } else {
      url = language + '/' + urlPostfix;
    }
    return getRoute(url);
  }

  function getMessages(route: Route): Messages {
    return route.data!['messages'];
  }

  function getRoute(routePath: string): Route {
    return routes.find(r => r.path === routePath)!;
  }
});
