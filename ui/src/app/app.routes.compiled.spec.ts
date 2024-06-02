export const compiledRoute = [
  {
    path: '',
    title: 'Mondkalender',
    component: 'MainComponent',
    data: {messages: 'de', home: '', about: 'about'}
  },
  {
    path: 'en',
    title: 'Moon Calendar',
    component: 'MainComponent',
    data: {messages: 'en', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl',
    title: 'Maankalender',
    component: 'MainComponent',
    data: {messages: 'nl', home: 'nl', about: 'nl/about'}
  },
  {
    path: 'es',
    title: 'Calendario Lunar',
    component: 'MainComponent',
    data: {messages: 'es', home: 'es', about: 'es/about'}
  },
  {
    path: 'fr',
    title: 'Calendrier de la Lune',
    component: 'MainComponent',
    data: {messages: 'fr', home: 'fr', about: 'fr/about'}
  },
  {
    path: 'ro',
    title: 'Calendar lunar',
    component: 'MainComponent',
    data: {messages: 'ro', home: 'ro', about: 'ro/about'}
  },
  {
    path: 'about',
    title: 'Über diese Seite',
    component: 'AboutComponent',
    data: {messages: 'de', home: '', about: 'about'}
  },
  {
    path: 'en/about',
    title: 'About this site',
    component: 'AboutComponent',
    data: {messages: 'en', home: 'en', about: 'en/about'}
  },
  {
    path: 'nl/about',
    title: 'Over deze pagina',
    component: 'AboutComponent',
    data: {messages: 'nl', home: 'nl', about: 'nl/about'}
  },
  {
    path: 'es/about',
    title: 'Acerca de este sitio',
    component: 'AboutComponent',
    data: {messages: 'es', home: 'es', about: 'es/about'}
  },
  {
    path: 'fr/about',
    title: 'À propos de ce site',
    component: 'AboutComponent',
    data: {messages: 'fr', home: 'fr', about: 'fr/about'}
  },
  {
    path: 'ro/about',
    title: 'Despre acest site',
    component: 'AboutComponent',
    data: {messages: 'ro', home: 'ro', about: 'ro/about'}
  },
  {
    path: '**',
    title: undefined,
    component: 'NotFoundComponent',
    data: {messages: 'de', home: '', about: 'about'}
  },
]
