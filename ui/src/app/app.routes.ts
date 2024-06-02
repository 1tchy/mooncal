import {Route, Routes} from '@angular/router';
import {MainComponent} from "./main/main.component";
import {AboutComponent} from "./about/about.component";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import messagesES from "./messages.es.json";
import messagesFR from "./messages.fr.json";
import messagesRO from "./messages.ro.json";
import {NotFoundComponent} from "./not-found/not-found.component";
import {Messages} from "./messages";

function buildAllRoutes() {
  let allRoutes: Routes = [];
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, MainComponent, '', m => m.app.title)))
  getAllLanguagesMessages().forEach(messages => allRoutes.push(buildRoute(messages, AboutComponent, 'about', m => m.about.title)))
  allRoutes.push({
    path: '**',
    component: NotFoundComponent,
    data: {messages: messagesDE, de: '', en: 'en', nl: 'nl', es: 'es', fr: 'fr', ro: 'ro', home: '', about: 'about'}
  })
  return allRoutes;
}

function buildRoute(messages: Messages, component: any, page: string = '', titleFunction: (messages: Messages) => string): Route {
  let language = messages.lang.current;
  let data: { [key: string]: any } = {
    messages: messages,
    home: buildPath(language, ''),
    about: buildPath(language, 'about')
  };
  for (const otherLanguage of getAllLanguages()) {
    data[otherLanguage] = buildPath(otherLanguage, page);
  }
  return {
    path: data[language],
    title: titleFunction(messages),
    component: component,
    data: data
  };
}

function buildPath(language: string, page: string): string {
  if (language === 'de') {
    return page;
  } else if (page === '') {
    return language;
  } else {
    return language + '/' + page;
  }
}

function getAllLanguagesMessages(): Messages[] {
  return [messagesDE, messagesEN, messagesNL, messagesES, messagesFR, messagesRO];
}

function getAllLanguages(): string[] {
  return getAllLanguagesMessages().map(messages => messages.lang.current);
}

export const routes: Routes = buildAllRoutes();
export {getAllLanguages as getAllLanguages};
