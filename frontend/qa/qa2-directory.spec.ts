import { expect, test } from '@playwright/test';
import { createCompany, signIn, unique } from './people';

/** QA-2 — Companies, contacts and a contact's timeline. */
test.describe('QA-2 Companies and contacts', () => {
  test('QA-2.1 a company can be created and appears in the list', async ({ page }) => {
    await signIn(page, 'sam');
    const name = unique('Acme');

    await createCompany(page, name);

    await expect(page.getByTestId('company-list')).toContainText(name);
  });

  test('QA-2.2 a blank company name is refused in words', async ({ page }) => {
    await signIn(page, 'sam');
    await page.getByTestId('nav-directory').click();

    await page.getByTestId('company-name').fill('   ');
    await page.getByTestId('add-company').click();

    await expect(page.getByTestId('error')).toBeVisible();
  });

  test('QA-2.3 a contact can be created at a company and filtered to it', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Globex'));
    const contact = unique('Cara');

    await page.getByTestId('contact-company').selectOption({ label: company });
    await page.getByTestId('contact-name').fill(contact);
    await page.getByTestId('contact-email').fill(`cara${Date.now()}@example.com`);
    await page.getByTestId('add-contact').click();
    await expect(page.getByTestId('contact-list')).toContainText(contact);

    await page.getByTestId('contact-filter').selectOption({ label: company });

    await expect(page.getByTestId('contact-list')).toContainText(contact);
    await expect(page.locator('[data-testid="contact-list"] li')).toHaveCount(1);
  });

  test('QA-2.4 an address that is not an address is refused in words', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Initech'));

    await page.getByTestId('contact-company').selectOption({ label: company });
    await page.getByTestId('contact-name').fill('Cara');
    await page.getByTestId('contact-email').fill('not-an-address');
    await page.getByTestId('add-contact').click();

    await expect(page.getByTestId('error')).toContainText('not an email address');
  });

  test('QA-2.5 a note logged against a contact appears on that contacts timeline', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Umbrella'));
    const contact = unique('Dev');
    await page.getByTestId('contact-company').selectOption({ label: company });
    await page.getByTestId('contact-name').fill(contact);
    await page.getByTestId('contact-email').fill(`dev${Date.now()}@example.com`);
    await page.getByTestId('add-contact').click();
    await expect(page.getByTestId('contact-list')).toContainText(contact);

    await page.getByTestId('contact-filter').selectOption({ label: company });
    await page.getByTestId('open-contact').first().click();
    await page.getByTestId('contact-note').fill('prefers email');
    await page.getByTestId('add-note').click();

    await expect(page.getByTestId('timeline')).toContainText('prefers email');
    await expect(page.getByTestId('timeline')).toContainText('Sam Sales');
  });
});
