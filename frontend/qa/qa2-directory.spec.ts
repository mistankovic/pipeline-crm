import { expect, test } from '@playwright/test';
import { addContact, createCompany, createDeal, open, renameCompany, rowFor, signIn, unique } from './people';

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

  test('QA-2.6 a company can be renamed and the new name survives a reload', async ({ page }) => {
    await signIn(page, 'sam');
    const before = await createCompany(page, unique('Acme'));
    const after = unique('Acme Holdings');

    await renameCompany(page, before, after);

    await expect(page.getByTestId('company-list')).toContainText(after);
    await expect(page.getByTestId('company-list')).not.toContainText(before);
    await page.reload();
    await page.getByTestId('nav-directory').click();
    await expect(page.getByTestId('company-list')).toContainText(after);
  });

  test('QA-2.7 a deal follows its company across a rename', async ({ page }) => {
    await signIn(page, 'sam');
    const before = await createCompany(page, unique('Acme'));
    const title = unique('Renewal');
    await createDeal(page, { title, company: before, value: 12000 });

    const after = unique('Acme Holdings');
    await page.getByTestId('nav-directory').click();
    await renameCompany(page, before, after);

    await page.getByTestId('nav-board').click();
    await open(page, title);
    await expect(page.getByTestId('deal-company-name')).toHaveText(after);
  });

  test('QA-2.8 a company cannot be renamed to nothing', async ({ page }) => {
    await signIn(page, 'sam');
    const name = await createCompany(page, unique('Initech'));

    await rowFor(page, 'company-list', name).getByTestId('rename-company').click();
    await page.getByTestId('company-new-name').fill('   ');
    await page.getByTestId('save-company').click();

    await expect(page.getByTestId('error')).toBeVisible();
    // The row is still an open form, so cancel out of it before reading the name back.
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByTestId('company-list')).toContainText(name);
  });

  test('QA-2.9 a contact can be corrected and the correction survives a reload', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Globex'));
    const before = await addContact(page, company, unique('Cara'));
    const after = unique('Cara Nguyen');
    const email = `nguyen${Date.now()}@example.com`;

    await rowFor(page, 'contact-list', before).getByTestId('edit-contact').click();
    await page.getByTestId('contact-new-name').fill(after);
    await page.getByTestId('contact-new-email').fill(email);
    await page.getByTestId('save-contact').click();

    await expect(page.getByTestId('contact-list')).toContainText(after);
    await expect(page.getByTestId('contact-list')).toContainText(email);
    await page.reload();
    await page.getByTestId('nav-directory').click();
    await expect(page.getByTestId('contact-list')).toContainText(after);
  });

  test('QA-2.10 a correction to an invalid address is refused in the domains words', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Umbrella'));
    const name = await addContact(page, company, unique('Dev'));

    await rowFor(page, 'contact-list', name).getByTestId('edit-contact').click();
    await page.getByTestId('contact-new-email').fill('not-an-address');
    await page.getByTestId('save-contact').click();

    await expect(page.getByTestId('error')).toContainText('not an email address');
  });

  test('QA-2.11 correcting a name does not move the person to another company', async ({ page }) => {
    await signIn(page, 'sam');
    const company = await createCompany(page, unique('Stark'));
    const before = await addContact(page, company, unique('Pep'));
    const after = unique('Pepper');

    await rowFor(page, 'contact-list', before).getByTestId('edit-contact').click();
    await page.getByTestId('contact-new-name').fill(after);
    await page.getByTestId('save-contact').click();
    await expect(page.getByTestId('contact-list')).toContainText(after);

    await page.getByTestId('contact-filter').selectOption({ label: company });

    await expect(page.getByTestId('contact-list')).toContainText(after);
    await expect(page.locator('[data-testid="contact-list"] li')).toHaveCount(1);
  });
});
