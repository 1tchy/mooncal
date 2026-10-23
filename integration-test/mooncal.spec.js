import {expect, test} from '@playwright/test';

// Stands in for matomo.js: takes over the _paq command queue like Matomo does, but only records the commands
const MATOMO_MOCK = `
  (function () {
    const calls = window.matomoMockCalls = [..._paq];
    _paq = {push: (...commands) => calls.push(...commands)};
  })();`;

/** Resolves with the next calendar API response (/mooncal?...) and its decoded query. */
function nextCalendarResponse(page) {
  return page.waitForResponse(r => new URL(r.url()).pathname === '/mooncal')
    .then(async r => {
      expect(r.ok(), `calendar request failed: ${r.status()} ${r.url()}`).toBeTruthy();
      return decodeURIComponent(r.url());
    });
}

/** Tracked Matomo events as 'category/action' strings. */
function trackedEvents(page) {
  return page.evaluate(() => window.matomoMockCalls
    .filter(command => command[0] === 'trackEvent')
    .map(command => command[1] + '/' + command[2]));
}

const calendarRows = page => page.locator('#calendar tbody tr');

test('moon calendar user journey', async ({page}) => {
  // Mock Matomo, so test runs never show up in the production statistics
  await page.route('https://mat.laurinmurer.ch/matomo.js', route => route.fulfill({contentType: 'text/javascript', body: MATOMO_MOCK}));
  await page.route('https://mat.laurinmurer.ch/matomo.php**', route => route.fulfill({status: 204}));
  // Keep the test hermetic: block everything else not served by the local server
  await page.route(url => !['localhost', '127.0.0.1', 'mat.laurinmurer.ch'].includes(url.hostname), route => route.abort());

  await test.step('open English calendar', async () => {
    const initial = nextCalendarResponse(page);
    await page.goto('/en/calendar');
    await initial;
    await expect(page.locator('h1')).toBeVisible();
    await expect(calendarRows(page).first()).toBeVisible();
    await page.waitForFunction(() => window.matomoMockCalls !== undefined);
    expect(await page.evaluate(() => window.matomoMockCalls)).toContainEqual(['setSiteId', '2']);
  });

  const initialRows = await calendarRows(page).count();

  await test.step('enable quarter moons', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#quarter-checkbox').click();
    expect(await response).toContain('phases[quarter]=true');
    await expect.poll(() => calendarRows(page).count()).toBeGreaterThan(initialRows);
  });

  const withQuarterRows = await calendarRows(page).count();

  await test.step('disable full moons', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#full-checkbox').click();
    expect(await response).not.toContain('phases[full]=true');
    await expect.poll(() => calendarRows(page).count()).toBeLessThan(withQuarterRows);
  });

  await test.step('disable lunar eclipses', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#lunareclipse-checkbox').click();
    const url = await response;
    expect(url).not.toContain('events[lunareclipse]=true');
    expect(url).toContain('events[solareclipse]=true');
    await expect(page.locator('#lunareclipse-checkbox')).not.toBeChecked();
    await expect(page).toHaveScreenshot('calendar-customized.png');
  });

  const modal = page.locator('.modal-content');

  await test.step('open "Add to Calendar" dialog', async () => {
    await page.getByRole('button', {name: 'Add to Calendar'}).click();
    await expect(modal).toBeVisible();
    await expect(page.locator('#icalLink')).toHaveValue(/\/mooncal\.ics\?created=\d+&.*phases\[quarter\]=true/);
    expect(await trackedEvents(page)).toContain('Calendar/openSubscriptionModal');
  });

  await test.step('switch to Google Calendar instructions', async () => {
    const tab = modal.getByRole('tab', {name: 'Google Calendar'});
    await tab.click();
    await expect(tab).toHaveClass(/active/);
    await expect(modal.locator('a[href="https://calendar.google.com/"]')).toBeVisible();
    // The iCal link contains the current time (created=...): replace only that part for the screenshot
    await page.locator('#icalLink').evaluate(link => link.value = link.value.replace(/created=\d+/, 'created=TIMESTAMP'));
    await expect(page).toHaveScreenshot('subscribe-dialog-google.png');
  });

  await test.step('close dialog', async () => {
    await modal.getByRole('button', {name: 'Close'}).click();
    await expect(modal).toBeHidden();
  });

  await test.step('open language menu', async () => {
    await page.locator('#languagesDropdown').click();
    await expect(page.locator('.dropdown-menu')).toBeVisible();
  });

  await test.step('switch to German', async () => {
    await page.locator('.dropdown-menu').getByText('Deutsch').click();
    await expect(page).toHaveURL(/localhost:\d+\/$/);
    await expect(page.locator('#phases .card-header')).toHaveText('Mondphasen');
    // Settings survive the language switch
    await expect(page.locator('#quarter-checkbox')).toBeChecked();
    await expect(page.locator('#full-checkbox')).not.toBeChecked();
    await expect(calendarRows(page).first()).toBeVisible();
    expect(await trackedEvents(page)).toContain('Settings/languageChange');
    await expect(page).toHaveScreenshot('german-calendar.png');
  });

  await test.step('open garden calendar', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('.navbar').getByRole('link', {name: 'Gartenkalender', exact: true}).click();
    await expect(page).toHaveURL(/\/gartenkalender$/);
    expect(await response).toContain('events[garden-biodynamic]=true');
    await expect(calendarRows(page).first()).toBeVisible();
    await expect(page).toHaveScreenshot('garden-calendar.png');
  });

  await test.step('open about page', async () => {
    await page.locator('.navbar').getByRole('link', {name: 'Über', exact: true}).click();
    await expect(page).toHaveURL(/\/ueber$/);
    await expect(page).toHaveTitle(/Über den Mondkalender/);
    await expect(page).toHaveScreenshot('about.png');
  });

  await test.step('enable daily phases with moonrise and moonset', async () => {
    const initial = nextCalendarResponse(page);
    await page.goto('/en/calendar');
    await initial;
    const response = nextCalendarResponse(page);
    await page.locator('#daily-checkbox').click();
    // defaults to the reference city of the browser's time zone
    expect(await response).toContain('&lat=47.4&lon=8.5');
    await expect(page.locator('#location-city')).toHaveText('≈ Zurich');
    await expect(calendarRows(page).first()).toContainText('↑');
  });

  await test.step('use the browser location', async () => {
    // Brest (France): same times as Zurich, Paris is the nearest reference city
    await page.context().grantPermissions(['geolocation']);
    await page.context().setGeolocation({latitude: 48.39123, longitude: -4.48654});
    const response = nextCalendarResponse(page);
    await page.locator('#use-my-location').click();
    const url = await response;
    expect(url).toContain('&lat=48.4&lon=-4.5');
    expect(url).not.toContain('48.39');
    expect(url).toContain('Europe/Paris');
    await expect(page.locator('#zone')).toHaveValue(/Europe\/Paris/);
    await expect(page.locator('#location-city')).toHaveText('≈ Your location');
    expect(JSON.stringify(await page.evaluate(() => window.matomoMockCalls))).not.toContain('48.4');
    await expect(page).toHaveScreenshot('daily-with-location.png');
  });

  await test.step('enable sunrise and sunset', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#sun-checkbox').click();
    const url = await response;
    expect(url).toContain('&phases[sun]=true');
    expect(url).toContain('&lat=48.4&lon=-4.5');
    // each day has the moon entry first, then the sun entry
    await expect(calendarRows(page).nth(1)).toContainText('Sun ↑');
    await expect(page).toHaveScreenshot('daily-and-sun-with-location.png');
  });
});
