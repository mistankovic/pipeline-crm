package com.pipelinecrm.tools.crap;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class CrapGate {

    static final int EXIT_OK = 0;
    static final int EXIT_VIOLATION = 1;
    static final int EXIT_USAGE = 2;

    private final PrintStream out;
    private final PrintStream err;
    private final JacocoReportParser parser;
    private final CrapEvaluator evaluator;
    private final ProductionClassDetector classDetector;

    public CrapGate() {
        this(System.out, System.err, new JacocoReportParser(), new CrapEvaluator(), new ProductionClassDetector());
    }

    CrapGate(
            PrintStream out,
            PrintStream err,
            JacocoReportParser parser,
            CrapEvaluator evaluator,
            ProductionClassDetector classDetector) {
        this.out = out;
        this.err = err;
        this.parser = parser;
        this.evaluator = evaluator;
        this.classDetector = classDetector;
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    static int execute(String[] args) {
        return new CrapGate().run(args);
    }

    public int run(String[] args) {
        GateArguments arguments;
        try {
            arguments = GateArguments.parse(args);
        } catch (IllegalArgumentException ex) {
            err.println(ex.getMessage());
            return EXIT_USAGE;
        }
        try {
            return evaluate(arguments);
        } catch (CrapCheckException ex) {
            err.println(ex.getMessage());
            return EXIT_USAGE;
        }
    }

    private int evaluate(GateArguments arguments) {
        if (!Files.exists(arguments.report())) {
            return handleMissingReport(arguments);
        }
        List<MethodCoverage> methods = parser.parse(arguments.report());
        CrapReport report = evaluator.evaluate(methods, arguments.threshold(), arguments.packages());
        printReport(report);
        return report.passed() ? EXIT_OK : EXIT_VIOLATION;
    }

    private int handleMissingReport(GateArguments arguments) {
        Path classesDir = arguments.classesDir();
        if (classDetector.hasExecutableClasses(classesDir)) {
            err.println("JaCoCo report missing but production classes exist: " + arguments.report());
            return EXIT_VIOLATION;
        }
        out.println("No JaCoCo report and no production classes; CRAP gate skipped.");
        return EXIT_OK;
    }

    private void printReport(CrapReport report) {
        out.printf(Locale.ROOT, "CRAP gate (threshold %.1f)%n", report.threshold());
        out.printf(Locale.ROOT, "checked: %d methods%n", report.checkedMethodCount());
        out.printf(Locale.ROOT, "violations: %d%n", report.violations().size());
        for (CrapViolation violation : report.violations()) {
            MethodCoverage method = violation.method();
            out.printf(
                    Locale.ROOT,
                    "VIOLATION %s complexity=%d coverage=%.2f crap=%.3f%n",
                    method.identity(),
                    method.complexity(),
                    method.lineCoverageRatio(),
                    violation.crap());
        }
        if (report.passed()) {
            out.println("OK");
        }
    }
}
