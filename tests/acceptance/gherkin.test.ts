import { describe, expect, it } from "vitest";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { loadFeatures } from "./gherkin";
import { newWorld, runStep } from "./world";

const here = dirname(fileURLToPath(import.meta.url));
const featuresDir = join(here, "../../features");
const features = loadFeatures(featuresDir);

describe("Gherkin acceptance", () => {
  for (const feature of features) {
    describe(feature.name, () => {
      for (const scenario of feature.scenarios) {
        it(scenario.name, async () => {
          const world = newWorld();
          const steps = [...feature.background, ...scenario.steps];
          for (const step of steps) {
            await runStep(world, step.text);
          }
          expect(true).toBe(true);
        });
      }
    });
  }
});
