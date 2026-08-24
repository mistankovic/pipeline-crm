package com.pipelinecrm.tools.crap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GateArguments {

    static final double DEFAULT_THRESHOLD = 6.0;

    private final Path report;
    private final Path classesDir;
    private final double threshold;
    private final List<String> packages;

    GateArguments(Path report, Path classesDir, double threshold, List<String> packages) {
        this.report = report;
        this.classesDir = classesDir;
        this.threshold = threshold;
        this.packages = List.copyOf(packages);
    }

    public Path report() {
        return report;
    }

    public Path classesDir() {
        return classesDir;
    }

    public double threshold() {
        return threshold;
    }

    public List<String> packages() {
        return packages;
    }

    public static GateArguments parse(String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("args must not be null");
        }
        return new Parser().parse(args);
    }

    private static final class Parser {
        private Path report;
        private Path classesDir;
        private double threshold = DEFAULT_THRESHOLD;
        private final List<String> packages = new ArrayList<>();

        private GateArguments parse(String[] args) {
            int index = 0;
            while (index < args.length) {
                index = consume(args, index);
            }
            return build();
        }

        private int consume(String[] args, int index) {
            String flag = args[index];
            return switch (flag) {
                case "--report" -> setReport(args, index);
                case "--classes-dir" -> setClassesDir(args, index);
                case "--threshold" -> setThreshold(args, index);
                case "--package" -> addPackage(args, index);
                default -> throw new IllegalArgumentException("unknown argument: " + flag);
            };
        }

        private int setReport(String[] args, int index) {
            report = Path.of(valueAfter(args, index, "--report"));
            return index + 2;
        }

        private int setClassesDir(String[] args, int index) {
            classesDir = Path.of(valueAfter(args, index, "--classes-dir"));
            return index + 2;
        }

        private int setThreshold(String[] args, int index) {
            String raw = valueAfter(args, index, "--threshold");
            try {
                threshold = Require.notNegative(Double.parseDouble(raw), "threshold");
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("threshold must be a number: " + raw, ex);
            }
            return index + 2;
        }

        private int addPackage(String[] args, int index) {
            packages.add(valueAfter(args, index, "--package"));
            return index + 2;
        }

        private GateArguments build() {
            if (report == null) {
                throw new IllegalArgumentException("--report is required");
            }
            return new GateArguments(report, classesDir, threshold, packages);
        }

        private static String valueAfter(String[] args, int index, String flag) {
            int valueIndex = index + 1;
            if (valueIndex >= args.length) {
                throw new IllegalArgumentException(flag + " requires a value");
            }
            return args[valueIndex];
        }
    }
}
