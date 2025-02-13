import {Route, Routes} from '@angular/router';
import {MainComponent} from "./main/main.component";
import {AboutComponent} from "./about/about.component";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import messagesES from "./messages.es.json";
import messagesFR from "./messages.fr.json";
import messagesRO from "./messages.ro.json";
import messagesHI from "./messages.hi.json";
import {NotFoundComponent} from "./not-found/not-found.component";
import {Messages} from "./messages";
import {ThankComponent} from "./thank/thank.component";

function buildAllRoutes() {
  let allRoutes: Routes = [];
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, MainComponent, m => m.navigation.paths.home, 'home', m => m.app.title)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, AboutComponent, m => m.navigation.paths.about, 'about', m => m.app.title + " - " + m.about.title)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, ThankComponent, m => m.navigation.paths.buymeacoffee, 'buymeacoffee', m => m.app.title + " - " + m.thank.title)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, ThankComponent, m => m.navigation.paths.thank, 'thank', m => m.app.title + " - " + m.thank.title)))
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
    component: NotFoundComponent,
    data: allRoutes[0].data
  })
  return allRoutes;
}

function buildRoute(messages: Messages, component: any, pathFunction: (messages: Messages) => string, id: string, titleFunction: (messages: Messages) => string): Route {
  let language = messages.lang.current;
  let data: { [key: string]: any } = {
    messages: messages,
    id: id,
    home: messages.navigation.paths.home,
    about: messages.navigation.paths.about,
    thank: messages.navigation.paths.thank
  };
  for (const otherLanguagesMessage of getAllLanguagesMessages()) {
    data[otherLanguagesMessage.lang.current] = pathFunction(otherLanguagesMessage)
  }
  return {
    path: data[language],
    title: titleFunction(messages),
    component: component,
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
