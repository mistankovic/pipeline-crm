package com.pipelinecrm.tools.crap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CrapEvaluator {

    public CrapReport evaluate(
            List<MethodCoverage> methods, double threshold, Collection<String> packagePrefixes) {
        requireMethods(methods);
        requirePrefixes(packagePrefixes);
        List<CrapViolation> violations = new ArrayList<>();
        int checked = 0;
        for (MethodCoverage method : methods) {
            if (!shouldCheck(method, packagePrefixes)) {
                continue;
            }
            checked++;
            double crap = CrapCalculator.crap(method.complexity(), method.lineCoverageRatio());
            if (CrapCalculator.exceeds(crap, threshold)) {
                violations.add(new CrapViolation(method, crap));
            }
        }
        return new CrapReport(checked, threshold, violations);
    }

    private static boolean shouldCheck(MethodCoverage method, Collection<String> packagePrefixes) {
        return method.isExecutable() && inPackages(method.packageName(), packagePrefixes);
    }

    private static boolean inPackages(String packageName, Collection<String> packagePrefixes) {
        if (packagePrefixes.isEmpty()) {
            return true;
        }
        for (String prefix : packagePrefixes) {
            if (matchesPrefix(packageName, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPrefix(String packageName, String prefix) {
        return packageName.equals(prefix) || packageName.startsWith(prefix + ".");
    }

    private static void requireMethods(List<MethodCoverage> methods) {
        Require.notNull(methods, "methods");
    }

    private static void requirePrefixes(Collection<String> packagePrefixes) {
        Require.notNull(packagePrefixes, "packagePrefixes");
    }
}
