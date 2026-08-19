import {Route, Routes} from '@angular/router';
import {MainComponent} from "./main/main.component";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import messagesES from "./messages.es.json";
import messagesFR from "./messages.fr.json";
import messagesRO from "./messages.ro.json";
import messagesHI from "./messages.hi.json";
import {Messages} from "./messages";

// Only the landing pages (MainComponent) ship in the initial bundle; everything else
// is split into lazy chunks that the router loads on demand.
type ComponentRoute = Pick<Route, 'component' | 'loadComponent'>;
const main: ComponentRoute = {component: MainComponent};
const about: ComponentRoute = {loadComponent: () => import('./about/about.component').then(m => m.AboutComponent)};
const thank: ComponentRoute = {loadComponent: () => import('./thank/thank.component').then(m => m.ThankComponent)};
const notFound: ComponentRoute = {loadComponent: () => import('./not-found/not-found.component').then(m => m.NotFoundComponent)};

function buildAllRoutes() {
  let allRoutes: Routes = [];
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, main, m => m.navigation.paths.home, 'home', m => m.app.pageTitle, m => m.app.description)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, main, m => m.navigation.paths.garden, 'garden', m => m.garden.pageTitle, m => m.garden.description)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, about, m => m.navigation.paths.about, 'about', m => m.about.pageTitle, m => m.about.description)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, thank, m => m.navigation.paths.buymeacoffee, 'buymeacoffee', m => m.thank.pageTitle, m => m.thank.description)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, thank, m => m.navigation.paths.thank, 'thank', m => m.thank.pageTitle, m => m.thank.description)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push({
    path: messages.navigation.paths.donate,
    redirectTo: messages.navigation.paths.thank,
    pathMatch: 'full'
  }))
  allRoutes.push({path: 'about', redirectTo: messagesDE.navigation.paths.about, pathMatch: 'full'})
  allRoutes.push({path: 'en', redirectTo: messagesEN.navigation.paths.home, pathMatch: 'full'})
  allRoutes.push({path: 'es', redirectTo: messagesES.navigation.paths.home, pathMatch: 'full'})
  allRoutes.push({path: 'es/about', redirectTo: messagesES.navigation.paths.about, pathMatch: 'full'})
  allRoutes.push({path: 'fr', redirectTo: messagesFR.navigation.paths.home, pathMatch: 'full'})
  allRoutes.push({path: 'fr/about', redirectTo: messagesFR.navigation.paths.about, pathMatch: 'full'})
  allRoutes.push({path: 'nl', redirectTo: messagesNL.navigation.paths.home, pathMatch: 'full'})
  allRoutes.push({path: 'nl/about', redirectTo: messagesNL.navigation.paths.about, pathMatch: 'full'})
  allRoutes.push({path: 'ro', redirectTo: messagesRO.navigation.paths.home, pathMatch: 'full'})
  allRoutes.push({path: 'ro/about', redirectTo: messagesRO.navigation.paths.about, pathMatch: 'full'})
  allRoutes.push({
    path: '**',
    ...notFound,
    data: allRoutes[0].data
  })
  return allRoutes;
}

function buildRoute(messages: Messages, componentRoute: ComponentRoute, pathFunction: (messages: Messages) => string, id: string, titleFunction: (messages: Messages) => string, descriptionFunction: (messages: Messages) => string): Route {
  let language = messages.lang.current;
  let data: { [key: string]: any } = {
    messages: messages,
    id: id,
    description: descriptionFunction(messages),
    home: messages.navigation.paths.home,
    about: messages.navigation.paths.about,
    garden: messages.navigation.paths.garden,
    thank: messages.navigation.paths.thank
  };
  for (const otherLanguagesMessage of getAllLanguagesMessages()) {
    data[otherLanguagesMessage.lang.current] = pathFunction(otherLanguagesMessage)
  }
  return {
    path: data[language],
    title: titleFunction(messages),
    ...componentRoute,
    data: data
  };
}

function getAllLanguagesMessages(): Messages[] {
  return [messagesDE, messagesEN, messagesNL, messagesES, messagesFR, messagesRO, messagesHI];
}

function getAllLanguages(): string[] {
  return getAllLanguagesMessages().map(messages => messages.lang.current);
}

function getAllLanguagesAndItsNames(): { [key: string]: string } {
  return getAllLanguagesMessages().reduce((accumulator, messages) => ({
    ...accumulator,
    [messages.lang.current]: messages.lang.currentName
  }), {});
}

export const routes: Routes = buildAllRoutes();
export {getAllLanguages as getAllLanguages};
export {getAllLanguagesAndItsNames as getAllLanguagesAndItsNames};
