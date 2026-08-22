import { expect, test } from '@playwright/test';
import { cardFor, column, createCompany, createDeal, open, signIn, unique } from './people';

/**
 * QA-5 — Who may move a deal.
 *
 * Two people, so the procedure needs two browser contexts. This is the procedure that would
 * have caught the Stage 6 defects: a rival being silently ignored, and being invited to try.
 */
test.describe('QA-5 Ownership', () => {
  test('QA-5.1 a rival salesperson is not invited to move the deal, and is told why', async ({ browser }) => {
    const samsPage = await browser.newPage();
    await signIn(samsPage, 'sam');
    const company = await createCompany(samsPage, unique('Acme'));
    const title = unique('Sams deal');
    await createDeal(samsPage, { title, company, value: 4000 });

    const robinsPage = await browser.newPage();
    await signIn(robinsPage, 'robin');
    await expect(cardFor(robinsPage, title)).toHaveAttribute('draggable', 'false');

    await cardFor(robinsPage, title).dragTo(column(robinsPage, 'QUALIFIED'));
    await expect(cardFor(robinsPage, title)).toHaveAttribute('data-stage', 'LEAD');

    await open(robinsPage, title);
    await expect(robinsPage.locator('[data-testid^="move-to-"]')).toHaveCount(0);
    await expect(robinsPage.getByTestId('not-yours')).toContainText('Sam Sales owns this deal');
    await expect(robinsPage.getByTestId('revise-value')).toHaveCount(0);

    await samsPage.close();
    await robinsPage.close();
  });

  test('QA-5.2 the owner may still move it', async ({ browser }) => {
    const samsPage = await browser.newPage();
    await signIn(samsPage, 'sam');
    const company = await createCompany(samsPage, unique('Acme'));
    const title = unique('Sams deal');
    await createDeal(samsPage, { title, company, value: 4000 });

    await expect(cardFor(samsPage, title)).toHaveAttribute('draggable', 'true');
    await cardFor(samsPage, title).dragTo(column(samsPage, 'QUALIFIED'));
    await expect(cardFor(samsPage, title)).toHaveAttribute('data-stage', 'QUALIFIED');

    await samsPage.close();
  });

  test('QA-5.3 a manager may move somebody elses deal', async ({ browser }) => {
    const samsPage = await browser.newPage();
    await signIn(samsPage, 'sam');
    const company = await createCompany(samsPage, unique('Acme'));
    const title = unique('Sams deal');
    await createDeal(samsPage, { title, company, value: 4000 });

    const mosPage = await browser.newPage();
    await signIn(mosPage, 'mo');
    await expect(cardFor(mosPage, title)).toHaveAttribute('draggable', 'true');

    await open(mosPage, title);
    await mosPage.getByTestId('move-to-QUALIFIED').click();
    await expect(mosPage.getByTestId('deal-stage')).toHaveText('Qualified');

    await samsPage.close();
    await mosPage.close();
  });

  test('QA-5.4 a rival can still read the deal and its timeline', async ({ browser }) => {
    const samsPage = await browser.newPage();
    await signIn(samsPage, 'sam');
    const company = await createCompany(samsPage, unique('Acme'));
    const title = unique('Readable');
    await createDeal(samsPage, { title, company, value: 4000 });
    await open(samsPage, title);
    await samsPage.getByTestId('activity-summary').fill('spoke to procurement');
    await samsPage.getByTestId('log-activity').click();
    await expect(samsPage.getByTestId('timeline')).toContainText('spoke to procurement');

    const robinsPage = await browser.newPage();
    await signIn(robinsPage, 'robin');
    await open(robinsPage, title);

    await expect(robinsPage.getByTestId('timeline')).toContainText('spoke to procurement');
    await expect(robinsPage.getByTestId('deal-value')).toContainText('4,000');

    await samsPage.close();
    await robinsPage.close();
  });
});
