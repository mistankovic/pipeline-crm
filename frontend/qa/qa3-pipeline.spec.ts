import { expect, test } from '@playwright/test';
import { cardFor, column, createCompany, createDeal, open, signIn, unique } from './people';

/** QA-3 — The board: creating deals, moving them, and the moves that are not offered. */
test.describe('QA-3 The pipeline board', () => {
  test('QA-3.1 a new deal appears as a lead with its weighted value', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Renewal');

    await createDeal(page, { title, company, value: 12000, probability: 50 });

    const card = cardFor(page, title);
    await expect(card).toHaveAttribute('data-stage', 'LEAD');
    await expect(card).toContainText('€12,000');
    await expect(card).toContainText('€6,000');
  });

  test('QA-3.2 a deal can be dragged one stage forward', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Renewal');
    await createDeal(page, { title, company, value: 5000 });

    await cardFor(page, title).dragTo(column(page, 'QUALIFIED'));

    await expect(cardFor(page, title)).toHaveAttribute('data-stage', 'QUALIFIED');
  });

  test('QA-3.3 a stage cannot be skipped, and the column does not invite it', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Renewal');
    await createDeal(page, { title, company, value: 5000 });

    await cardFor(page, title).dragTo(column(page, 'PROPOSAL'));

    await expect(cardFor(page, title)).toHaveAttribute('data-stage', 'LEAD');
  });

  test('QA-3.4 the detail screen offers only the moves the server allows', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Renewal');
    await createDeal(page, { title, company, value: 5000 });

    await open(page, title);

    await expect(page.getByTestId('move-to-QUALIFIED')).toBeVisible();
    await expect(page.getByTestId('move-to-CLOSED_LOST')).toBeVisible();
    await expect(page.getByTestId('move-to-CLOSED_WON')).toHaveCount(0);
    await expect(page.getByTestId('move-to-PROPOSAL')).toHaveCount(0);
  });

  test('QA-3.5 losing a deal from lead forces its probability to zero', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const title = unique('Doomed');
    await createDeal(page, { title, company, value: 5000, probability: 80 });

    await open(page, title);
    await page.getByTestId('move-to-CLOSED_LOST').click();

    await expect(page.getByTestId('deal-stage')).toHaveText('Closed Lost');
    await expect(page.getByTestId('deal-probability')).toHaveText('0%');
  });

  test('QA-3.6 the board can be narrowed to one owner', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));
    const mine = unique('Mine');
    await createDeal(page, { title: mine, company, value: 1000 });
    const theirs = unique('Theirs');
    await createDeal(page, { title: theirs, company, value: 1000, owner: 'Robin Reid' });

    await page.getByTestId('owner-filter').selectOption({ label: 'Robin Reid' });

    await expect(cardFor(page, theirs)).toBeVisible();
    await expect(page.locator('[data-testid="deal-card"]').filter({ hasText: mine })).toHaveCount(0);
  });
});
