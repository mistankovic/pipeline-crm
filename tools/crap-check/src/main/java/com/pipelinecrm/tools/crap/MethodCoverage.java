package com.pipelinecrm.tools.crap;

public record MethodCoverage(
        String packageName,
        String className,
        String methodName,
        String descriptor,
        int complexity,
        int linesCovered,
        int linesMissed) {

    public MethodCoverage {
        Require.notNull(packageName, "packageName");
        Require.notNull(className, "className");
        Require.notNull(methodName, "methodName");
        Require.notNull(descriptor, "descriptor");
        Require.notNegative(complexity, "complexity");
        Require.notNegative(linesCovered, "linesCovered");
        Require.notNegative(linesMissed, "linesMissed");
    }

    public int linesTotal() {
        return linesCovered + linesMissed;
    }

    public boolean isExecutable() {
        return complexity > 0 && linesTotal() > 0;
    }

    public double lineCoverageRatio() {
        int total = linesTotal();
        if (total == 0) {
            return 1.0;
        }
        return (double) linesCovered / total;
    }

    public String identity() {
        return packageName + "." + simpleClassName() + "#" + methodName + descriptor;
    }

    private String simpleClassName() {
        int slash = className.lastIndexOf('/');
        if (slash < 0) {
            return className;
        }
        return className.substring(slash + 1);
    }
}
