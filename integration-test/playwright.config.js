import {defineConfig, devices} from '@playwright/test';

export default defineConfig({
  testDir: '.',
  testMatch: '*.spec.js',
  timeout: 120_000,
  retries: 0,
  workers: 1,
  reporter: [['list'], ['html', {open: 'never', outputFolder: 'report'}]],
  outputDir: 'test-results',
  // Committed reference screenshots; any difference fails the test (update with run.sh --update-screenshots)
  snapshotPathTemplate: '{testDir}/screenshots/{arg}{ext}',
  expect: {toHaveScreenshot: {threshold: 0, maxDiffPixels: 0}},
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:9123',
    locale: 'en-US',
    timezoneId: 'Europe/Zurich',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{name: 'chromium', use: {...devices['Desktop Chrome'], viewport: {width: 1280, height: 900}}}],
});
