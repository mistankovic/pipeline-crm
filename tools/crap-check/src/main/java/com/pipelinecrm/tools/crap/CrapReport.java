package com.pipelinecrm.tools.crap;

import java.util.List;

public record CrapReport(int checkedMethodCount, double threshold, List<CrapViolation> violations) {

    public CrapReport {
        Require.notNegative(checkedMethodCount, "checkedMethodCount");
        Require.notNegative(threshold, "threshold");
        Require.notNull(violations, "violations");
        violations = List.copyOf(violations);
    }

    public boolean passed() {
        return violations.isEmpty();
    }
}
