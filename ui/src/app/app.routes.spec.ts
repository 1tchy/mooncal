import {getAllLanguages, routes} from './app.routes';
import {compiledRoute} from './app.routes.compiled.spec';
import {Route} from "@angular/router";
import {Messages} from "./messages";

describe('Routes', () => {
  it('not found route data should equal to german home', () => {
    expect(getRoute('**').data).toEqual(getRoute('').data);
  });
  it('all pages link to their main and about page', () => {
    for (const route of routesWithoutRedirect()) {
      let messages = getMessages(route);
      expect(route.data!['home']).withContext(messages.lang.current).toEqual(messages.navigation.paths.home);
      expect(route.data!['about']).withContext(messages.lang.current).toEqual(messages.navigation.paths.about);
    }
  });
  it('all pages link to all other languages', () => {
    for (const route of routesWithoutRedirect().filter(r => r.path !== '**')) {
      let language = getMessages(route).lang.current;
      for (const otherLanguage of getAllLanguages()) {
        if (otherLanguage === language) {
          continue;
        }
        let pathOfOtherLanguage = route.data?.[otherLanguage];
        expect(pathOfOtherLanguage).withContext(otherLanguage + ' in ' + language).toBeDefined();
        expect(getRoute(pathOfOtherLanguage).component?.name).withContext(pathOfOtherLanguage).toEqual(route.component?.name);
      }
    }
  });
  it('all paths are unique', () => {
    let allPaths = routes.map(r => r.path!);
    let uniquePaths = new Set(allPaths);
    expect(uniquePaths.size).toEqual(allPaths.length);
  });
  it('all titles are translated', () => {
    routes
      .filter(r => r.redirectTo === undefined)
      .filter(r => r.path !== '**')
      .forEach(r => {
        expect(r.title!.length).withContext(r.path!).toBeGreaterThan(0)
      });
  });
  it('all components have all languages', () => {
    for (const component of new Set(routesWithoutRedirect().filter(r => r.path !== '**').map(r => r.component!))) {
      for (const language of getAllLanguages()) {
        expect(routesWithoutRedirect().filter(r => !r.path!.endsWith('buymeacoffee')).filter(r => r.component === component && getMessages(r).lang.current === language).length).withContext(component + ' in ' + language).toBe(1);
      }
    }
  });
  it('all routes do not accidentally change', () => {
    const routesFormatted = routes.map(r => {
      if (r.redirectTo) {
        return {
          redirectFrom: r.path,
          redirectTo: r.redirectTo
        };
      }
      return {
        path: r.path,
        title: r.title,
        component: r.component?.name,
        data: {
          messages: r.data?.['messages'].lang.current,
          home: r.data?.['home'],
          about: r.data?.['about'],
        }
      }
    });
    expect(routesFormatted).toEqual(compiledRoute);
  });

  function routesWithoutRedirect(): Route[] {
    return routes.filter(r => !r.redirectTo);
  }

  function getMessages(route: Route): Messages {
    return route.data!['messages'];
  }

  function getRoute(routePath: string): Route {
    return routes.find(r => r.path === routePath)!;
  }
});
