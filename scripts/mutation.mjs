#!/usr/bin/env node
/**
 * Focused mutation testing on rule-bearing domain files.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { spawnSync } from "node:child_process";

const files = [
  "src/domain/deal.ts",
  "src/domain/deal-stage.ts",
  "src/domain/activity.ts",
  "src/domain/actor.ts",
  "src/domain/money.ts",
  "src/domain/forecast.ts",
].map((rel) => join(process.cwd(), rel));

const replacements = [
  ["!this.value.isPositive()", "this.value.isPositive()"],
  ["if (!hasConversation)", "if (false)"],
  ["this.isTerminal()", "false"],
  ["this.userId === ownerId", "this.userId !== ownerId"],
  ["this.minorUnits > 0", "this.minorUnits > 1"],
  ["deal.isOpen()", "true"],
  ["this.isOnDeal(dealId) && this.type.isConversation()", "this.isOnDeal(dealId)"],
];

function runTests() {
  const result = spawnSync(
    "npx",
    ["vitest", "run", "src/domain", "src/application", "tests/acceptance", "--reporter=dot"],
    { encoding: "utf8", timeout: 60_000 },
  );
  return result.status === 0;
}

const survivors = [];
let killed = 0;

for (const file of files) {
  const original = readFileSync(file, "utf8");
  for (const [from, to] of replacements) {
    if (!original.includes(from)) continue;
    const mutated = original.replace(from, to);
    if (mutated === original) continue;
    writeFileSync(file, mutated);
    let survived = false;
    try {
      survived = runTests();
    } finally {
      writeFileSync(file, original);
    }
    if (survived) survivors.push({ file: file.split("/src/")[1], from, to });
    else killed += 1;
  }
}

console.log(`Mutation testing: killed ${killed}`);
if (survivors.length) {
  console.error("SURVIVORS:");
  for (const item of survivors) console.error(`  ${item.file}: ${item.from} → ${item.to}`);
  process.exit(1);
}
console.log("OK: no surviving relevant mutants");
