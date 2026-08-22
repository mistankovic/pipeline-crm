import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";

export type StepKind = "Given" | "When" | "Then" | "And" | "But";

export type Step = { kind: StepKind; text: string; line: number };

export type Scenario = {
  name: string;
  steps: Step[];
  line: number;
};

export type Feature = {
  name: string;
  file: string;
  background: Step[];
  scenarios: Scenario[];
};

export function loadFeatures(dir: string): Feature[] {
  return readdirSync(dir)
    .filter((name) => name.endsWith(".feature"))
    .sort()
    .map((name) => parseFeature(join(dir, name), readFileSync(join(dir, name), "utf8")));
}

export function parseFeature(file: string, source: string): Feature {
  const lines = source.split(/\r?\n/);
  let name = "Unnamed";
  const background: Step[] = [];
  const scenarios: Scenario[] = [];
  let current: Scenario | null = null;
  let target: Step[] = background;

  for (let i = 0; i < lines.length; i++) {
    const line = (lines[i] ?? "").trim();
    if (!line || line.startsWith("#")) continue;
    if (line.startsWith("Feature:")) {
      name = line.slice("Feature:".length).trim();
      continue;
    }
    if (line.startsWith("Background:")) {
      current = null;
      target = background;
      continue;
    }
    if (line.startsWith("Scenario:")) {
      current = {
        name: line.slice("Scenario:".length).trim(),
        steps: [],
        line: i + 1,
      };
      scenarios.push(current);
      target = current.steps;
      continue;
    }
    const match = /^(Given|When|Then|And|But)\s+(.+)$/.exec(line);
    if (match) {
      target.push({
        kind: match[1] as StepKind,
        text: match[2] ?? "",
        line: i + 1,
      });
    }
  }
  return { name, file, background, scenarios };
}
