#!/usr/bin/env node
/**
 * CRAP(m) = comp(m)^2 * (1 - cov(m))^3 + comp(m)
 * Target: every method in domain + application ≤ 6.
 */
import { readFileSync, readdirSync, statSync, existsSync } from "node:fs";
import { join } from "node:path";

const roots = [join(process.cwd(), "src/domain"), join(process.cwd(), "src/application")];
const coveragePath = join(process.cwd(), "coverage/coverage-final.json");

function walk(dir) {
  const out = [];
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) out.push(...walk(path));
    else if (path.endsWith(".ts") && !path.endsWith(".test.ts") && !path.endsWith("index.ts") && !path.endsWith("ports.ts")) {
      out.push(path);
    }
  }
  return out;
}

function methods(source) {
  const found = [];
  const pattern =
    /(?:(?:export\s+)?(?:async\s+)?function\s+(\w+)|(?:(?:public|private|static|async)\s+)+(\w+)\s*\(|^\s{2}(?:async\s+)?(\w+)\s*\()/gm;
  let match;
  while ((match = pattern.exec(source))) {
    const name = match[1] || match[2] || match[3];
    if (!name || name === "if" || name === "for" || name === "while" || name === "switch") continue;
    const start = match.index;
    found.push({ name, start });
  }
  return found;
}

function sliceBody(source, start, nextStart) {
  return source.slice(start, nextStart);
}

function complexity(body) {
  const tokens = body.match(/\b(if|else if|case|for|while|catch|\?|&&|\|\|)\b/g) ?? [];
  return 1 + tokens.length;
}

function coverageForFile(report, file) {
  const entry = Object.values(report).find((item) => String(item.path ?? "").endsWith(file.replace(/\\/g, "/").split("/src/")[1] ? "/src/" + file.split("/src/")[1] : file));
  if (!entry?.s) return 1;
  const values = Object.values(entry.s);
  if (!values.length) return 1;
  const hit = values.filter((n) => n > 0).length;
  return hit / values.length;
}

const files = roots.flatMap(walk);
const report = existsSync(coveragePath) ? JSON.parse(readFileSync(coveragePath, "utf8")) : {};
const offenders = [];
const rows = [];

for (const file of files) {
  const source = readFileSync(file, "utf8");
  const fns = methods(source);
  const cov = coverageForFile(report, file);
  for (let i = 0; i < fns.length; i++) {
    const fn = fns[i];
    const body = sliceBody(source, fn.start, fns[i + 1]?.start ?? source.length);
    const comp = complexity(body);
    const crap = comp * comp * (1 - cov) ** 3 + comp;
    const rounded = Number(crap.toFixed(2));
    rows.push({ file, name: fn.name, comp, cov: Number(cov.toFixed(3)), crap: rounded });
    if (rounded > 6) offenders.push({ file, name: fn.name, crap: rounded, comp });
  }
}

rows.sort((a, b) => b.crap - a.crap);
console.log("CRAP report (domain + application)");
for (const row of rows.slice(0, 20)) {
  console.log(
    `${row.crap.toFixed(2).padStart(6)}  cc=${String(row.comp).padStart(2)}  cov=${row.cov.toFixed(2)}  ${row.name}  ${row.file.split("/src/")[1]}`,
  );
}
if (offenders.length) {
  console.error(`\nFAIL: ${offenders.length} method(s) exceed CRAP 6`);
  for (const item of offenders) console.error(`  ${item.name} = ${item.crap} (cc ${item.comp})`);
  process.exit(1);
}
console.log(`\nOK: ${rows.length} methods, all CRAP ≤ 6`);
