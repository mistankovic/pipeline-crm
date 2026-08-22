import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const root = join(process.cwd(), "src");

function walk(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    const path = join(dir, entry);
    const stat = statSync(path);
    if (stat.isDirectory()) out.push(...walk(path));
    else if (path.endsWith(".ts") && !path.endsWith(".test.ts")) out.push(path);
  }
  return out;
}

function importsOf(source: string): string[] {
  const found: string[] = [];
  const pattern = /from\s+["']([^"']+)["']/g;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(source))) {
    found.push(match[1] ?? "");
  }
  return found;
}

const forbiddenInDomain = [
  "react",
  "@tanstack",
  "zod",
  "pg",
  "kysely",
  "better-auth",
  "@/lib/db",
  "@/lib/auth",
  "../adapters",
  "../application",
  "../routes",
  "../components",
  "../lib/",
];

const forbiddenInApplication = [
  "react",
  "@tanstack",
  "pg",
  "better-auth",
  "@/lib/db",
  "@/lib/auth",
  "../adapters",
  "../routes",
  "../components",
];

describe("Dependency rule", () => {
  it("keeps domain free of frameworks and outer layers", () => {
    const files = walk(join(root, "domain"));
    expect(files.length).toBeGreaterThan(0);
    const leaks: string[] = [];
    for (const file of files) {
      const source = readFileSync(file, "utf8");
      for (const spec of importsOf(source)) {
        if (forbiddenInDomain.some((bit) => spec.includes(bit))) {
          leaks.push(`${relative(root, file)} imports ${spec}`);
        }
      }
    }
    expect(leaks).toEqual([]);
  });

  it("keeps application free of frameworks and adapters", () => {
    const files = walk(join(root, "application"));
    expect(files.length).toBeGreaterThan(0);
    const leaks: string[] = [];
    for (const file of files) {
      const source = readFileSync(file, "utf8");
      for (const spec of importsOf(source)) {
        if (forbiddenInApplication.some((bit) => spec.includes(bit))) {
          leaks.push(`${relative(root, file)} imports ${spec}`);
        }
        if (spec.startsWith("../../adapters") || spec.includes("/adapters/")) {
          leaks.push(`${relative(root, file)} imports ${spec}`);
        }
      }
    }
    expect(leaks).toEqual([]);
  });
});
