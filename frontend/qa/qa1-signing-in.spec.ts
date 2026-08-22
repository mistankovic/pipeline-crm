import { expect, test } from '@playwright/test';
import { PEOPLE, signIn } from './people';

/** QA-1 — Signing in and out. See docs/qa/procedures.md. */
test.describe('QA-1 Signing in', () => {
  test('QA-1.1 a correct password reaches the pipeline', async ({ page }) => {
    await signIn(page, 'sam');

    await expect(page.getByTestId('signed-in-as')).toContainText('SALES');
    await expect(page.locator('[data-testid="stage-column"]')).toHaveCount(6);
  });

  test('QA-1.2 a wrong password is refused, in words, and nothing opens', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.getByTestId('email').fill(PEOPLE.sam.email);
    await page.getByTestId('password').fill('not-sams-password');
    await page.getByTestId('sign-in').click();

    await expect(page.getByTestId('error')).toHaveText('email address or password is incorrect');
    await expect(page.getByTestId('sign-in')).toBeVisible();
  });

  test('QA-1.3 an unknown address is refused identically, revealing nothing', async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.getByTestId('email').fill('nobody@pipelinecrm.demo');
    await page.getByTestId('password').fill(PEOPLE.sam.password);
    await page.getByTestId('sign-in').click();

    await expect(page.getByTestId('error')).toHaveText('email address or password is incorrect');
  });

  test('QA-1.4 the session survives a reload', async ({ page }) => {
    await signIn(page, 'sam');

    await page.reload();

    await expect(page.getByTestId('signed-in-as')).toContainText('Sam Sales');
  });

  test('QA-1.5 signing out returns to the sign-in screen', async ({ page }) => {
    await signIn(page, 'sam');

    await page.getByTestId('sign-out').click();

    await expect(page.getByTestId('sign-in')).toBeVisible();
  });

  test('QA-1.6 a tampered token ends the session rather than stranding the user', async ({ page }) => {
    await signIn(page, 'sam');

    await page.evaluate(() => {
      const stored = JSON.parse(localStorage.getItem('pipelinecrm.session')!);
      stored.token = `${stored.token.slice(0, -6)}AAAAAA`;
      localStorage.setItem('pipelinecrm.session', JSON.stringify(stored));
    });
    await page.reload();

    await expect(page.getByTestId('sign-in')).toBeVisible();
  });
});
