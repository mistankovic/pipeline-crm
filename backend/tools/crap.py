#!/usr/bin/env python3
"""CRAP metric gate.

CRAP(m) = comp(m)^2 * (1 - cov(m))^3 + comp(m)

where comp is the cyclomatic complexity JaCoCo reports for the method and cov is the
fraction of the method's lines that the test suite covered.

Reads a JaCoCo XML report, computes CRAP per method, writes a Markdown report and exits
non-zero when any method exceeds the threshold. See CONSTITUTION.md section 3.1.
"""
from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

EXIT_OK = 0
EXIT_VIOLATION = 1
EXIT_NO_REPORT = 2


@dataclass(frozen=True)
class MethodMetric:
    owner: str
    name: str
    signature: str
    complexity: int
    covered_lines: int
    total_lines: int

    @property
    def coverage(self) -> float:
        if self.total_lines == 0:
            return 1.0
        return self.covered_lines / self.total_lines

    @property
    def crap(self) -> float:
        uncovered = 1.0 - self.coverage
        return self.complexity ** 2 * uncovered ** 3 + self.complexity

    @property
    def display_name(self) -> str:
        return f"{self.owner}.{self.name}{self.signature}"


def _counter(element: ElementTree.Element, counter_type: str) -> tuple[int, int]:
    for counter in element.findall("counter"):
        if counter.get("type") == counter_type:
            return int(counter.get("missed", "0")), int(counter.get("covered", "0"))
    return 0, 0


def _method_metric(class_name: str, method: ElementTree.Element) -> MethodMetric:
    missed_complexity, covered_complexity = _counter(method, "COMPLEXITY")
    missed_lines, covered_lines = _counter(method, "LINE")
    return MethodMetric(
        owner=class_name.replace("/", "."),
        name=method.get("name", "?"),
        signature=method.get("desc", ""),
        complexity=missed_complexity + covered_complexity,
        covered_lines=covered_lines,
        total_lines=missed_lines + covered_lines,
    )


def read_report(report_path: Path) -> tuple[str, list[MethodMetric]]:
    root = ElementTree.parse(report_path).getroot()
    metrics = []
    for class_element in root.iter("class"):
        class_name = class_element.get("name", "?")
        for method in class_element.findall("method"):
            metrics.append(_method_metric(class_name, method))
    return root.get("name", "unknown module"), metrics


def render_report(metrics: list[MethodMetric], threshold: float, module: str) -> str:
    ranked = sorted(metrics, key=lambda metric: (-metric.crap, metric.display_name))
    violations = [metric for metric in ranked if metric.crap > threshold]
    lines = [
        f"# CRAP report — {module}",
        "",
        f"Threshold: {threshold} · methods analysed: {len(metrics)} · violations: {len(violations)}",
        "",
        "| CRAP | Complexity | Line coverage | Method |",
        "|-----:|-----------:|--------------:|--------|",
    ]
    for metric in ranked[:20]:
        lines.append(
            f"| {metric.crap:.2f} | {metric.complexity} | "
            f"{metric.coverage * 100:.1f}% | `{metric.display_name}` |"
        )
    if not ranked:
        lines.append("| — | — | — | _no methods analysed_ |")
    lines.append("")
    return "\n".join(lines)


def parse_arguments(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fail the build when any method exceeds the CRAP threshold.")
    parser.add_argument("jacoco_xml", type=Path)
    parser.add_argument("--threshold", type=float, default=6.0)
    parser.add_argument("--report", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: list[str]) -> int:
    arguments = parse_arguments(argv)
    if not arguments.jacoco_xml.exists():
        print(f"CRAP gate: no JaCoCo report at {arguments.jacoco_xml}", file=sys.stderr)
        return EXIT_NO_REPORT

    module, metrics = read_report(arguments.jacoco_xml)
    arguments.report.parent.mkdir(parents=True, exist_ok=True)
    arguments.report.write_text(render_report(metrics, arguments.threshold, module), encoding="utf-8")

    violations = [metric for metric in metrics if metric.crap > arguments.threshold]
    if violations:
        print(f"CRAP gate FAILED for {module}: {len(violations)} method(s) above {arguments.threshold}", file=sys.stderr)
        for metric in sorted(violations, key=lambda item: -item.crap):
            print(
                f"  CRAP {metric.crap:6.2f}  complexity {metric.complexity:2d}  "
                f"coverage {metric.coverage * 100:5.1f}%  {metric.display_name}",
                file=sys.stderr,
            )
        return EXIT_VIOLATION

    worst = max(metrics, key=lambda item: item.crap, default=None)
    worst_text = f"{worst.crap:.2f} ({worst.display_name})" if worst else "n/a"
    print(f"CRAP gate passed for {module}: {len(metrics)} methods, worst {worst_text}")
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
