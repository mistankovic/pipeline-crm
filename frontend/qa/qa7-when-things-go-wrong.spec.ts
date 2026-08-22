import { expect, test } from '@playwright/test';
import { createCompany, signIn, unique } from './people';

/**
 * QA-7 — When things go wrong.
 *
 * Added after hostile QA found a request a signed-in user could make and be **logged out** by:
 * an unhandled server fault was re-dispatched anonymously and answered 401, and the browser
 * correctly ends a session on a 401. The procedures walked every happy path and every business
 * refusal, and never asked what happens when something actually breaks.
 * See docs/reviews/stage-7-review.md, findings F-7.1 and F-7.3.
 */
test.describe('QA-7 When things go wrong', () => {
  test('QA-7.1 an absurd value is refused in words, and you stay signed in', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Acme'));

    await page.getByTestId('nav-board').click();
    await page.getByTestId('new-deal').click();
    await page.getByTestId('deal-title').fill(unique('Absurd'));
    await page.getByTestId('deal-company').selectOption({ label: company });
    await page.getByTestId('deal-value').fill('99999999999999999999');
    await page.getByTestId('create-deal').click();

    await expect(page.getByTestId('error')).toContainText('must not exceed');
    await expect(page.getByTestId('signed-in-as')).toContainText('Sam Sales');
  });

  test('QA-7.2 a company name that looks like SQL is stored and shown as text', async ({ page }) => {
    await signIn(page, 'sam');
    const nasty = `Bobby${Date.now()}'); DROP TABLE deals;--`;

    await createCompany(page, nasty);

    await expect(page.getByTestId('company-list')).toContainText(nasty);

    // The actual assertion: the deals table still exists and the board still works.
    await page.getByTestId('nav-board').click();
    await expect(page.locator('[data-testid="stage-column"]')).toHaveCount(6);
  });

  test('QA-7.3 a company name containing markup is shown, not executed', async ({ page }) => {
    await signIn(page, 'sam');
    const markup = `Acme${Date.now()}<img src=x onerror="window.__owned=1">`;
    let scriptRan = false;

    await createCompany(page, markup);
    scriptRan = await page.evaluate(() => 'window' in globalThis && '__owned' in window);

    await expect(page.getByTestId('company-list')).toContainText(markup);
    expect(scriptRan, 'the markup must be shown as text, never rendered').toBe(false);
  });
});
