import { expect, test } from '@playwright/test';
import { createCompany, createDeal, open, signIn, unique } from './people';

/** QA-4 — Winning a deal, and being told why it cannot be won yet. */
test.describe('QA-4 Winning', () => {
  async function walkToNegotiation(page: import('@playwright/test').Page, title: string) {
    for (const stage of ['QUALIFIED', 'PROPOSAL', 'NEGOTIATION']) {
      await page.getByTestId(`move-to-${stage}`).click();
      await expect(page.getByTestId('deal-stage')).toHaveText(
        stage.charAt(0) + stage.slice(1).toLowerCase()
      );
    }
  }

  test('QA-4.1 a deal with no call or meeting cannot be won, and the reason is shown', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Unproven');
    await createDeal(page, { title, company, value: 9000 });
    await open(page, title);
    await walkToNegotiation(page, title);

    await page.getByTestId('move-to-CLOSED_WON').click();

    await expect(page.getByTestId('error')).toContainText('no call or meeting has been logged');
    await expect(page.getByTestId('deal-stage')).toHaveText('Negotiation');
  });

  test('QA-4.2 logging a meeting then winning succeeds and forces the probability to 100', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Winnable');
    await createDeal(page, { title, company, value: 9000, probability: 30 });
    await open(page, title);
    await walkToNegotiation(page, title);

    await page.getByTestId('activity-type-input').selectOption('MEETING');
    await page.getByTestId('activity-summary').fill('agreed terms over lunch');
    await page.getByTestId('log-activity').click();
    await expect(page.getByTestId('timeline')).toContainText('agreed terms over lunch');

    await page.getByTestId('move-to-CLOSED_WON').click();

    await expect(page.getByTestId('deal-stage')).toHaveText('Closed Won');
    await expect(page.getByTestId('deal-probability')).toHaveText('100%');
  });

  test('QA-4.3 a won deal offers no further moves and cannot be revised', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Done');
    await createDeal(page, { title, company, value: 9000 });
    await open(page, title);
    await walkToNegotiation(page, title);
    await page.getByTestId('activity-type-input').selectOption('CALL');
    await page.getByTestId('activity-summary').fill('confirmed by phone');
    await page.getByTestId('log-activity').click();
    await page.getByTestId('move-to-CLOSED_WON').click();
    await expect(page.getByTestId('deal-stage')).toHaveText('Closed Won');

    await expect(page.locator('[data-testid^="move-to-"]')).toHaveCount(0);
    await page.getByTestId('revise-value').fill('99999');
    await page.getByTestId('save-value').click();
    await expect(page.getByTestId('error')).toContainText('can no longer be changed');
    await expect(page.getByTestId('deal-value')).toContainText('9,000');
  });

  test('QA-4.4 the timeline records what happened, in order, with who did it', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Chatty');
    await createDeal(page, { title, company, value: 1000 });
    await open(page, title);

    for (const [kind, what] of [
      ['NOTE', 'first contact made'],
      ['CALL', 'talked about pricing']
    ] as const) {
      await page.getByTestId('activity-type-input').selectOption(kind);
      await page.getByTestId('activity-summary').fill(what);
      await page.getByTestId('log-activity').click();
      await expect(page.getByTestId('timeline')).toContainText(what);
    }

    const kinds = await page.locator('[data-testid="activity-type"]').allTextContents();
    expect(kinds).toEqual(['NOTE', 'CALL']);
    await expect(page.getByTestId('timeline')).toContainText('Sam Sales');
  });
});
