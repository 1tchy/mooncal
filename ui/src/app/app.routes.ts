import {Routes} from '@angular/router';
import {MainComponent} from "./main/main.component";
import {AboutComponent} from "./about/about.component";
import messagesDE from "./messages.de.json";
import messagesEN from "./messages.en.json";
import messagesNL from "./messages.nl.json";
import {NotFoundComponent} from "./not-found/not-found.component";

export const routes: Routes = [
  {
    path: '',
    title: 'Mondkalender',
    component: MainComponent,
    data: {messages: messagesDE, en: 'en', nl: 'nl', home: '', about: 'about'}
  },
  {
    path: 'en',
    title: 'Moon Calendar',
    component: MainComponent,
    data: {messages: messagesEN, de: '', nl: 'nl', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl',
    title: 'Maankalender',
    component: MainComponent,
    data: {messages: messagesNL, de: '', en: 'en', home: 'nl', about: 'nl/about'}
  },
  {
    path: 'about',
    title: 'Über mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesDE, en: 'en/about', nl: 'nl/about', home: '', about: 'about'}
  },
  {
    path: 'en/about',
    title: 'About mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesEN, de: 'about', nl: 'nl/about', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl/about',
    title: 'Over mooncal.ch',
    component: AboutComponent,
    data: {messages: messagesNL, de: 'about', en: 'en/about', home: 'nl', about: 'nl/about'}
  },
  {
    path: '**',
    component: NotFoundComponent,
    data: {messages: messagesDE, en: 'en', nl: 'nl', home: '', about: 'about'}
  }
];
