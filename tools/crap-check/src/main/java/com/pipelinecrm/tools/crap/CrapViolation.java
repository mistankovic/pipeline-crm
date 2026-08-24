package com.pipelinecrm.tools.crap;

public record CrapViolation(MethodCoverage method, double crap) {

    public CrapViolation {
        Require.notNull(method, "method");
        Require.notNegative(crap, "crap");
    }
}
