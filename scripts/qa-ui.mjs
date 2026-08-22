import { chromium } from "playwright";
import { mkdirSync } from "node:fs";

mkdirSync("/workspace/screenshots", { recursive: true });

const browser = await chromium.launch({ args: ["--no-sandbox"] });
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
const email = `qa.${Date.now()}@pipeline.test`;
const password = "Pipeline99!";
const dealTitle = `Zero touch ${Date.now()}`;

try {
  await page.goto("http://127.0.0.1:8080/login", { waitUntil: "networkidle" });
  await page.getByRole("button", { name: /need an account/i }).click();
  await page.locator('input[name="name"]').fill("QA Operator");
  await page.locator('input[name="email"]').fill(email);
  await page.locator('input[name="password"]').fill(password);
  await page.getByRole("button", { name: "Create account" }).click();
  await page.getByRole("heading", { name: "The board" }).waitFor({ timeout: 30_000 });
  await page.screenshot({ path: "/workspace/screenshots/qa-board.png", fullPage: true });

  await page.getByRole("button", { name: "New deal" }).click();
  await page.locator('input[name="title"]').fill(dealTitle);
  const companyValue = await page.locator('select[name="companyId"] option').nth(1).getAttribute("value");
  if (!companyValue) throw new Error("No company to attach a deal to");
  await page.locator('select[name="companyId"]').selectOption(companyValue);
  await page.locator('input[name="amount"]').fill("25000");
  await page.locator('input[name="probability"]').fill("15");
  await page.getByRole("button", { name: "Save deal" }).click();
  await page.locator("article").filter({ hasText: dealTitle }).waitFor({ timeout: 15_000 });
  await page.locator("article").filter({ hasText: dealTitle }).click();
  await page.getByRole("heading", { level: 1, name: dealTitle }).waitFor({ timeout: 15_000 });
  await page.screenshot({ path: "/workspace/screenshots/qa-deal.png", fullPage: true });

  await page.getByRole("button", { name: "Move to Won" }).click();
  await page.getByText(/Call or Meeting|won deal/i).waitFor({ timeout: 8_000 });
  await page.screenshot({ path: "/workspace/screenshots/qa-rejected-won.png", fullPage: true });

  await page.locator('select[name="type"]').selectOption("MEETING");
  await page.locator('textarea[name="body"]').fill("Satellite review with Mina.");
  await page.getByRole("button", { name: "Log activity" }).click();
  await page.getByText("Satellite review with Mina.").waitFor({ timeout: 10_000 });
  await page.getByRole("button", { name: "Move to Won" }).click();
  await page.getByText("100%").waitFor({ timeout: 10_000 });
  await page.screenshot({ path: "/workspace/screenshots/qa-won.png", fullPage: true });

  await page.getByRole("link", { name: "Forecast" }).click();
  await page.getByRole("heading", { name: "Forecast" }).waitFor();
  await page.screenshot({ path: "/workspace/screenshots/qa-forecast.png", fullPage: true });

  await page.getByRole("link", { name: "Companies" }).click();
  await page.getByRole("heading", { name: "Companies" }).waitFor();
  await page.screenshot({ path: "/workspace/screenshots/qa-companies.png", fullPage: true });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole("link", { name: "Board" }).click();
  await page.getByRole("heading", { name: "The board" }).waitFor();
  await page.screenshot({ path: "/workspace/screenshots/qa-mobile-board.png", fullPage: true });

  console.log(JSON.stringify({ ok: true, email, dealTitle }));
} catch (error) {
  await page.screenshot({ path: "/workspace/screenshots/qa-failure.png", fullPage: true });
  console.error(error);
  process.exitCode = 1;
} finally {
  await browser.close();
}
