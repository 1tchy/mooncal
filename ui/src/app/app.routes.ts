import {Routes} from '@angular/router';
import {MainComponent} from "./main/main.component";
import {AboutComponent} from "./about/about.component";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import messagesES from "./messages.es.json";
import messagesFR from "./messages.fr.json";
import {NotFoundComponent} from "./not-found/not-found.component";

export const routes: Routes = [
  {
    path: '',
    title: 'Mondkalender',
    component: MainComponent,
    data: {messages: messagesDE, en: 'en', nl: 'nl', es: 'es', fr: 'fr', home: '', about: 'about'}
  },
  {
    path: 'en',
    title: 'Moon Calendar',
    component: MainComponent,
    data: {messages: messagesEN, de: '', nl: 'nl', es: 'es', fr: 'fr', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl',
    title: 'Maankalender',
    component: MainComponent,
    data: {messages: messagesNL, de: '', en: 'en', es: 'es', fr: 'fr', home: 'nl', about: 'nl/about'}
  },
  {
    path: 'es',
    title: 'Calendario Lunar',
    component: MainComponent,
    data: {messages: messagesES, de: '', en: 'en', nl: 'nl', fr: 'fr', home: 'es', about: 'es/about'}
  },
  {
    path: 'fr',
    title: 'Calendrier de la Lune',
    component: MainComponent,
    data: {messages: messagesFR, de: '', en: 'en', nl: 'nl', es: 'es', home: 'fr', about: 'fr/about'}
  },
  {
    path: 'about',
    title: 'Über mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesDE, en: 'en/about', nl: 'nl/about', es: 'es/about', fr: 'fr/about', home: '', about: 'about'}
  },
  {
    path: 'en/about',
    title: 'About mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesEN, de: 'about', nl: 'nl/about', es: 'es/about', fr: 'fr/about', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl/about',
    title: 'Over mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesNL, de: 'about', en: 'en/about', es: 'es/about', fr: 'fr/about', home: 'nl', about: 'nl/about'}
  },
  {
    path: 'es/about',
    title: 'Acerca de mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesES, de: 'about', en: 'en/about', nl: 'nl/about', fr: 'fr/about', home: 'es', about: 'es/about'}
  },
  {
    path: 'fr/about',
    title: 'À propos mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesFR, de: 'about', en: 'en/about', nl: 'nl/about', es: 'es/about', home: 'fr', about: 'fr/about'}
  },
  {
    path: '**',
    component: NotFoundComponent,
    data: {messages: messagesDE, en: 'en', nl: 'nl', es: 'es', fr: 'fr', home: '', about: 'about'}
  }
];
