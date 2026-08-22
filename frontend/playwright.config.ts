import { defineConfig, devices } from '@playwright/test';

/**
 * The QA procedures, run as a script.
 *
 * Every spec in `qa/` is the scripted form of a numbered procedure in `docs/qa/`. A human can
 * follow the document; this runs the same steps. Where the two ever disagree, the document is
 * the specification and the script is wrong.
 *
 * `PLAYWRIGHT_CHROMIUM_PATH` lets an environment point at a browser it already has instead of
 * downloading one. CI leaves it unset.
 */
const browser = process.env.PLAYWRIGHT_CHROMIUM_PATH;
const apiOrigin = process.env.API_ORIGIN ?? 'http://127.0.0.1:8080';

export default defineConfig({
  testDir: './qa',
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'retain-on-failure',
    launchOptions: browser ? { executablePath: browser } : {}
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'npx vite --host 127.0.0.1 --port 5173',
      url: 'http://127.0.0.1:5173',
      reuseExistingServer: true,
      timeout: 60_000,
      env: { API_ORIGIN: apiOrigin }
    }
  ]
});
