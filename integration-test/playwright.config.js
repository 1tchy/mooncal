import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: '.',
  testMatch: '*.spec.js',
  timeout: 120_000,
  retries: 0,
  workers: 1,
  reporter: [['list'], ['html', {open: 'never', outputFolder: 'report'}]],
  outputDir: 'test-results',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:9123',
    // German-speaking browser: with a non-German locale the app redirects German pages back to English
    locale: 'de-CH',
    timezoneId: 'Europe/Zurich',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{name: 'chromium', use: {...devices['Desktop Chrome'], viewport: {width: 1280, height: 900}}}],
});
