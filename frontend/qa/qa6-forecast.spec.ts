import { expect, test } from '@playwright/test';
import { createCompany, createDeal, open, signIn, unique } from './people';

/** QA-6 — Revising a deal, and the forecast. */
test.describe('QA-6 Revising and forecasting', () => {
  test('QA-6.1 repricing an open deal updates its weighted value', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Repriced');
    await createDeal(page, { title, company, value: 1000, probability: 50 });
    await open(page, title);

    await page.getByTestId('revise-value').fill('4000');
    await page.getByTestId('save-value').click();

    await expect(page.getByTestId('deal-value')).toContainText('4,000');
    await expect(page.locator('.figures')).toContainText('2,000');
  });

  test('QA-6.2 reweighting an open deal updates its weighted value', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Reweighted');
    await createDeal(page, { title, company, value: 1000, probability: 50 });
    await open(page, title);

    await page.getByTestId('revise-probability').fill('25');
    await page.getByTestId('save-probability').click();

    await expect(page.getByTestId('deal-probability')).toHaveText('25%');
    await expect(page.locator('.figures')).toContainText('250');
  });

  test('QA-6.3 the forecast groups by owner with readable names', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    await createDeal(page, { title: unique('Forecastable'), company, value: 2000, probability: 25 });

    await page.getByTestId('nav-forecast').click();
    await page.getByTestId('forecast-dimension').selectOption('OWNER');

    await expect(page.getByTestId('forecast-table')).toContainText('Sam Sales');
  });

  test('QA-6.4 the forecast can be grouped by stage instead', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    await createDeal(page, { title: unique('Leadish'), company, value: 2000, probability: 25 });

    await page.getByTestId('nav-forecast').click();
    await page.getByTestId('forecast-dimension').selectOption('STAGE');

    await expect(page.getByTestId('forecast-table')).toContainText('LEAD');
  });

  /**
   * The forecast line is a total over every open deal in that stage, and the demo database is
   * shared, so this procedure measures the *difference* rather than expecting a bare figure.
   * An earlier version asserted the literal amount and failed the moment another procedure had
   * left a lead behind — a test that only passed on an empty database.
   */
  test('QA-6.5 a lost deal leaves the forecast, taking its weighted value with it', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Vanishing');
    const worth = 777_000;

    const before = await leadTotal(page);
    await createDeal(page, { title, company, value: worth, probability: 100 });
    const withIt = await leadTotal(page);
    expect(withIt - before).toBe(worth);

    await page.getByTestId('nav-board').click();
    await open(page, title);
    await page.getByTestId('move-to-CLOSED_LOST').click();
    await expect(page.getByTestId('deal-stage')).toHaveText('Closed Lost');

    expect(await leadTotal(page)).toBe(before);
  });

  /** The weighted euro total of the LEAD line, or zero when there is no such line. */
  async function leadTotal(page: import('@playwright/test').Page): Promise<number> {
    await page.getByTestId('nav-forecast').click();
    await page.getByTestId('forecast-dimension').selectOption('STAGE');
    // Wait for the heading the *server* sent, not for the dropdown we just changed: otherwise
    // the previous grouping's rows are still on screen and the total read is the wrong one.
    await expect(page.getByTestId('forecast-grouping')).toHaveText('Stage');
    const line = page.locator('[data-testid="forecast-line"]').filter({ hasText: 'LEAD' }).first();
    if ((await line.count()) === 0) {
      return 0;
    }
    const shown = await line.locator('td').last().innerText();
    return Number(shown.replace(/[^0-9.-]/g, ''));
  }

  test('QA-6.6 two currencies produce two lines and are never added together', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    await page.getByTestId('nav-board').click();
    await page.getByTestId('new-deal').click();
    await page.getByTestId('deal-title').fill(unique('Dollars'));
    await page.getByTestId('deal-company').selectOption({ label: company });
    await page.getByTestId('deal-value').fill('1000');
    await page.getByTestId('deal-currency').fill('USD');
    await page.getByTestId('deal-probability').fill('100');
    await page.getByTestId('create-deal').click();

    await page.getByTestId('nav-forecast').click();
    await page.getByTestId('forecast-dimension').selectOption('STAGE');

    await expect(page.getByTestId('forecast-table')).toContainText('$');
  });
});
