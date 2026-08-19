import { RenderMode, ServerRoute } from '@angular/ssr';

// All landing pages are known at build time (localized paths per language),
// so every route is prerendered into a static index.html served by Play.
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
