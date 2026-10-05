import {expect, test} from '@playwright/test';

const SCREENSHOTS = 'screenshots';

/** Resolves with the next calendar API response (/mooncal?...) and its decoded query. */
function nextCalendarResponse(page) {
  return page.waitForResponse(r => new URL(r.url()).pathname === '/mooncal')
    .then(async r => {
      expect(r.ok(), `calendar request failed: ${r.status()} ${r.url()}`).toBeTruthy();
      return decodeURIComponent(r.url());
    });
}

const calendarRows = page => page.locator('#calendar tbody tr');

test('moon calendar user journey (10 clicks, 5 screenshots)', async ({page}) => {
  // Keep the test hermetic: block everything not served by the local server (e.g. Matomo analytics),
  // so test runs never show up in the production statistics.
  await page.route(url => !['localhost', '127.0.0.1'].includes(url.hostname), route => route.abort());

  await test.step('open English calendar', async () => {
    const initial = nextCalendarResponse(page);
    await page.goto('/en/calendar');
    await initial;
    await expect(page.locator('h1')).toBeVisible();
    await expect(calendarRows(page).first()).toBeVisible();
  });

  const initialRows = await calendarRows(page).count();

  await test.step('click 1: enable quarter moons', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#quarter-checkbox').click();
    expect(await response).toContain('phases[quarter]=true');
    await expect.poll(() => calendarRows(page).count()).toBeGreaterThan(initialRows);
  });

  const withQuarterRows = await calendarRows(page).count();

  await test.step('click 2: disable full moons', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#full-checkbox').click();
    expect(await response).not.toContain('phases[full]=true');
    await expect.poll(() => calendarRows(page).count()).toBeLessThan(withQuarterRows);
  });

  await test.step('click 3: disable lunar eclipses', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('#lunareclipse-checkbox').click();
    const url = await response;
    expect(url).not.toContain('events[lunareclipse]=true');
    expect(url).toContain('events[solareclipse]=true');
    await expect(page.locator('#lunareclipse-checkbox')).not.toBeChecked();
    await page.screenshot({path: `${SCREENSHOTS}/1-calendar-customized.png`});
  });

  const modal = page.locator('.modal-content');

  await test.step('click 4: open "Add to Calendar" dialog', async () => {
    await page.getByRole('button', {name: 'Add to Calendar'}).click();
    await expect(modal).toBeVisible();
    await expect(page.locator('#icalLink')).toHaveValue(/\/mooncal\.ics\?.*phases\[quarter\]=true/);
  });

  await test.step('click 5: switch to Google Calendar instructions', async () => {
    const tab = modal.getByRole('tab', {name: 'Google Calendar'});
    await tab.click();
    await expect(tab).toHaveClass(/active/);
    await expect(modal.locator('a[href="https://calendar.google.com/"]')).toBeVisible();
    await page.screenshot({path: `${SCREENSHOTS}/2-subscribe-dialog-google.png`});
  });

  await test.step('click 6: close dialog', async () => {
    await modal.getByRole('button', {name: 'Close'}).click();
    await expect(modal).toBeHidden();
  });

  await test.step('click 7: open language menu', async () => {
    await page.locator('#languagesDropdown').click();
    await expect(page.locator('.dropdown-menu')).toBeVisible();
  });

  await test.step('click 8: switch to German', async () => {
    await page.locator('.dropdown-menu').getByText('Deutsch').click();
    await expect(page).toHaveURL(/localhost:\d+\/$/);
    await expect(page.locator('#phases .card-header')).toHaveText('Mondphasen');
    // Settings survive the language switch
    await expect(page.locator('#quarter-checkbox')).toBeChecked();
    await expect(page.locator('#full-checkbox')).not.toBeChecked();
    await expect(calendarRows(page).first()).toBeVisible();
    await page.screenshot({path: `${SCREENSHOTS}/3-german-calendar.png`});
  });

  await test.step('click 9: open garden calendar', async () => {
    const response = nextCalendarResponse(page);
    await page.locator('.navbar').getByRole('link', {name: 'Gartenkalender', exact: true}).click();
    await expect(page).toHaveURL(/\/gartenkalender$/);
    expect(await response).toContain('events[garden-biodynamic]=true');
    await expect(calendarRows(page).first()).toBeVisible();
    await page.screenshot({path: `${SCREENSHOTS}/4-garden-calendar.png`});
  });

  await test.step('click 10: open about page', async () => {
    await page.locator('.navbar').getByRole('link', {name: 'Über', exact: true}).click();
    await expect(page).toHaveURL(/\/ueber$/);
    await expect(page).toHaveTitle(/Über den Mondkalender/);
    await page.screenshot({path: `${SCREENSHOTS}/5-about.png`});
  });
});
