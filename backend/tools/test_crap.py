"""Tests for the CRAP gate. The gate is build infrastructure, so it is tested like code."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).with_name("crap.py")

REPORT_TEMPLATE = """<?xml version="1.0" encoding="UTF-8"?>
<report name="module">
  <package name="com/pipelinecrm/domain">
    <class name="com/pipelinecrm/domain/Sample" sourcefilename="Sample.java">
      <method name="{name}" desc="()V" line="10">
        <counter type="LINE" missed="{missed_lines}" covered="{covered_lines}"/>
        <counter type="COMPLEXITY" missed="{missed_complexity}" covered="{covered_complexity}"/>
      </method>
    </class>
  </package>
</report>
"""


def _write_report(directory, **counters):
    path = Path(directory) / "jacoco.xml"
    path.write_text(REPORT_TEMPLATE.format(**counters), encoding="utf-8")
    return path


def _run(report_path, threshold="6"):
    report_out = report_path.with_name("crap-report.md")
    completed = subprocess.run(
        [sys.executable, str(SCRIPT), str(report_path), "--threshold", threshold, "--report", str(report_out)],
        capture_output=True,
        text=True,
        check=False,
    )
    return completed, report_out


class CrapGateTest(unittest.TestCase):

    def test_passes_when_a_complex_method_is_fully_covered(self):
        with tempfile.TemporaryDirectory() as directory:
            report = _write_report(
                directory, name="fullyCovered", missed_lines=0, covered_lines=10,
                missed_complexity=0, covered_complexity=6,
            )

            completed, _ = _run(report)

            self.assertEqual(0, completed.returncode, completed.stderr)

    def test_fails_when_a_complex_method_is_half_covered(self):
        with tempfile.TemporaryDirectory() as directory:
            report = _write_report(
                directory, name="halfCovered", missed_lines=5, covered_lines=5,
                missed_complexity=3, covered_complexity=3,
            )

            completed, _ = _run(report)

            self.assertEqual(1, completed.returncode)
            self.assertIn("halfCovered", completed.stderr)

    def test_passes_when_a_trivial_method_is_uncovered(self):
        with tempfile.TemporaryDirectory() as directory:
            report = _write_report(
                directory, name="trivialUncovered", missed_lines=2, covered_lines=0,
                missed_complexity=1, covered_complexity=0,
            )

            completed, _ = _run(report)

            self.assertEqual(0, completed.returncode, completed.stderr)

    def test_writes_a_markdown_report_listing_the_method(self):
        with tempfile.TemporaryDirectory() as directory:
            report = _write_report(
                directory, name="listed", missed_lines=0, covered_lines=4,
                missed_complexity=0, covered_complexity=2,
            )

            _, report_out = _run(report)

            self.assertIn("com.pipelinecrm.domain.Sample.listed", report_out.read_text(encoding="utf-8"))

    def test_reports_a_missing_jacoco_report_as_a_distinct_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            missing = Path(directory) / "absent.xml"

            completed, _ = _run(missing)

            self.assertEqual(2, completed.returncode)


if __name__ == "__main__":
    unittest.main()
