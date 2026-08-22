import { expect, type Page } from '@playwright/test';

/**
 * The three seeded users, and how to be one of them.
 *
 * Passwords are the demo passwords from the seed migration. They are in the README as well:
 * this is a demo, and pretending otherwise would make the procedures unfollowable.
 */
export const PEOPLE = {
  sam: { email: 'sam@pipelinecrm.demo', password: 'sam-password', name: 'Sam Sales' },
  robin: { email: 'robin@pipelinecrm.demo', password: 'robin-password', name: 'Robin Reid' },
  mo: { email: 'mo@pipelinecrm.demo', password: 'mo-password', name: 'Mo Mancini' }
} as const;

export type Person = keyof typeof PEOPLE;

/** Signs in from a clean browser state, and waits until the application is usable. */
export async function signIn(page: Page, who: Person): Promise<void> {
  const person = PEOPLE[who];
  await page.goto('/');
  await page.evaluate(() => localStorage.clear());
  await page.reload();
  await page.getByTestId('email').fill(person.email);
  await page.getByTestId('password').fill(person.password);
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('signed-in-as')).toContainText(person.name);
}

/** A name nothing else in the run will use, so procedures never interfere with each other. */
export function unique(prefix: string): string {
  return `${prefix} ${Date.now().toString(36)}-${Math.floor(Math.random() * 1e4)}`;
}

/** Creates a company and returns its name. */
export async function createCompany(page: Page, name: string): Promise<string> {
  await page.getByTestId('nav-directory').click();
  await page.getByTestId('company-name').fill(name);
  await page.getByTestId('add-company').click();
  await expect(page.getByTestId('company-list')).toContainText(name);
  return name;
}

/** Creates a deal on the board and opens nothing; returns its card. */
export async function createDeal(
  page: Page,
  options: { title: string; company: string; value: number; probability?: number; owner?: string }
): Promise<void> {
  await page.getByTestId('nav-board').click();
  await page.getByTestId('new-deal').click();
  await page.getByTestId('deal-title').fill(options.title);
  await page.getByTestId('deal-company').selectOption({ label: options.company });
  await page.getByTestId('deal-value').fill(String(options.value));
  await page.getByTestId('deal-probability').fill(String(options.probability ?? 50));
  if (options.owner) {
    await page.getByTestId('deal-owner').selectOption({ label: options.owner });
  }
  await page.getByTestId('create-deal').click();
  await expect(cardFor(page, options.title)).toBeVisible();
}

export function cardFor(page: Page, title: string) {
  return page.locator('[data-testid="deal-card"]').filter({ hasText: title }).first();
}

export function column(page: Page, stage: string) {
  return page.locator(`[data-testid="stage-column"][data-stage="${stage}"]`);
}

/** Opens a deal's detail screen. */
export async function open(page: Page, title: string): Promise<void> {
  await cardFor(page, title).click();
  await expect(page.getByTestId('deal-title')).toHaveText(title);
}
