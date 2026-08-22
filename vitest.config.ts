import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  resolve: {
    alias: {
      "@": join(root, "src"),
    },
  },
  test: {
    include: [
      "src/domain/**/*.test.ts",
      "src/application/**/*.test.ts",
      "tests/**/*.test.ts",
    ],
    coverage: {
      provider: "v8",
      reportsDirectory: join(root, "coverage"),
      include: ["src/domain/**/*.ts", "src/application/**/*.ts"],
      exclude: ["**/*.test.ts", "**/index.ts", "**/ports.ts"],
      reporter: ["text", "json", "json-summary"],
    },
  },
});
