export const compiledRoute = [
  {
    path: '',
    title: 'Mondkalender',
    component: 'MainComponent',
    data: {messages: 'de', home: '', about: 'ueber'}
  },
  {
    path: 'en/calendar',
    title: 'Moon Calendar',
    component: 'MainComponent',
    data: {messages: 'en', home: 'en/calendar', about: 'en/about'}
  },
  {
    path: 'nl/maankalender',
    title: 'Maankalender',
    component: 'MainComponent',
    data: {messages: 'nl', home: 'nl/maankalender', about: 'nl/over'}
  },
  {
    path: 'es/calendario-lunar',
    title: 'Calendario Lunar',
    component: 'MainComponent',
    data: {messages: 'es', home: 'es/calendario-lunar', about: 'es/acerca'}
  },
  {
    path: 'fr/calendrier-lune',
    title: 'Calendrier de la Lune',
    component: 'MainComponent',
    data: {messages: 'fr', home: 'fr/calendrier-lune', about: 'fr/a-propos'}
  },
  {
    path: 'ro/calendar-lunar',
    title: 'Calendar lunar',
    component: 'MainComponent',
    data: {messages: 'ro', home: 'ro/calendar-lunar', about: 'ro/despre'}
  },
  {
    path: 'hi/calendar',
    title: 'चंद्र कैलेंडर',
    component: 'MainComponent',
    data: {messages: 'hi', home: 'hi/calendar', about: 'hi/about'}
  },
  {
    path: 'ueber',
    title: 'Über diese Seite',
    component: 'AboutComponent',
    data: {messages: 'de', home: '', about: 'ueber'}
  },
  {
    path: 'en/about',
    title: 'About this site',
    component: 'AboutComponent',
    data: {messages: 'en', home: 'en/calendar', about: 'en/about'}
  },
  {
    path: 'nl/over',
    title: 'Over deze pagina',
    component: 'AboutComponent',
    data: {messages: 'nl', home: 'nl/maankalender', about: 'nl/over'}
  },
  {
    path: 'es/acerca',
    title: 'Acerca de este sitio',
    component: 'AboutComponent',
    data: {messages: 'es', home: 'es/calendario-lunar', about: 'es/acerca'}
  },
  {
    path: 'fr/a-propos',
    title: 'À propos de ce site',
    component: 'AboutComponent',
    data: {messages: 'fr', home: 'fr/calendrier-lune', about: 'fr/a-propos'}
  },
  {
    path: 'ro/despre',
    title: 'Despre acest site',
    component: 'AboutComponent',
    data: {messages: 'ro', home: 'ro/calendar-lunar', about: 'ro/despre'}
  },
  {
    path: 'hi/about',
    title: 'यह साइट के बारे में',
    component: 'AboutComponent',
    data: {messages: 'hi', home: 'hi/calendar', about: 'hi/about'}
  },
  {
    path: 'buymeacoffee',
    title: 'Über diese Seite',
    component: 'AboutComponent',
    data: {messages: 'de', home: '', about: 'ueber'}
  },
  {
    path: 'en/buymeacoffee',
    title: 'About this site',
    component: 'AboutComponent',
    data: {messages: 'en', home: 'en/calendar', about: 'en/about'}
  },
  {
    path: 'nl/buymeacoffee',
    title: 'Over deze pagina',
    component: 'AboutComponent',
    data: {messages: 'nl', home: 'nl/maankalender', about: 'nl/over'}
  },
  {
    path: 'es/buymeacoffee',
    title: 'Acerca de este sitio',
    component: 'AboutComponent',
    data: {messages: 'es', home: 'es/calendario-lunar', about: 'es/acerca'}
  },
  {
    path: 'fr/buymeacoffee',
    title: 'À propos de ce site',
    component: 'AboutComponent',
    data: {messages: 'fr', home: 'fr/calendrier-lune', about: 'fr/a-propos'}
  },
  {
    path: 'ro/buymeacoffee',
    title: 'Despre acest site',
    component: 'AboutComponent',
    data: {messages: 'ro', home: 'ro/calendar-lunar', about: 'ro/despre'}
  },
  {
    path: 'hi/buymeacoffee',
    title: 'यह साइट के बारे में',
    component: 'AboutComponent',
    data: {messages: 'hi', home: 'hi/calendar', about: 'hi/about'}
  },
  {
    redirectFrom: 'about',
    redirectTo: 'ueber'
  },
  {
    redirectFrom: 'en',
    redirectTo: 'en/calendar'
  },
  {
    redirectFrom: 'es',
    redirectTo: 'es/calendario-lunar'
  },
  {
    redirectFrom: 'es/about',
    redirectTo: 'es/acerca'
  },
  {
    redirectFrom: 'fr',
    redirectTo: 'fr/calendrier-lune'
  },
  {
    redirectFrom: 'fr/about',
    redirectTo: 'fr/a-propos'
  },
  {
    redirectFrom: 'nl',
    redirectTo: 'nl/maankalender'
  },
  {
    redirectFrom: 'nl/about',
    redirectTo: 'nl/over'
  },
  {
    redirectFrom: 'ro',
    redirectTo: 'ro/calendar-lunar'
  },
  {
    redirectFrom: 'ro/about',
    redirectTo: 'ro/despre'
  },
  {
    path: '**',
    title: undefined,
    component: 'NotFoundComponent',
    data: {messages: 'de', home: '', about: 'ueber'}
  },
]
