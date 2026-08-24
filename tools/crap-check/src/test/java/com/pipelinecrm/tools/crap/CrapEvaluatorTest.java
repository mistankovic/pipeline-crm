package com.pipelinecrm.tools.crap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CrapEvaluatorTest {

    private final CrapEvaluator evaluator = new CrapEvaluator();

    @Test
    void reportsViolationOnlyForMatchingExecutableMethodsOverThreshold() {
        MethodCoverage inner = MethodCoverageTest.sample("advance", 5, 5, 5);
        MethodCoverage otherPackage =
                new MethodCoverage("com.other", "com/other/X", "m", "()V", 9, 0, 9);
        MethodCoverage skipped = MethodCoverageTest.sample("<init>", 0, 0, 0);

        CrapReport report = evaluator.evaluate(
                List.of(inner, otherPackage, skipped), 6.0, List.of("com.pipelinecrm.domain"));

        assertThat(report.checkedMethodCount()).isEqualTo(1);
        assertThat(report.passed()).isFalse();
        assertThat(report.violations()).hasSize(1);
        assertThat(report.violations().getFirst().method()).isEqualTo(inner);
    }

    @Test
    void matchesNestedPackagesAndExactPrefix() {
        MethodCoverage nested = new MethodCoverage(
                "com.pipelinecrm.domain.model",
                "com/pipelinecrm/domain/model/Deal",
                "ok",
                "()V",
                1,
                1,
                0);
        CrapReport report = evaluator.evaluate(List.of(nested), 6.0, List.of("com.pipelinecrm.domain"));
        assertThat(report.checkedMethodCount()).isEqualTo(1);
        assertThat(report.passed()).isTrue();
    }

    @Test
    void doesNotTreatSiblingPackageAsMatch() {
        MethodCoverage sibling = new MethodCoverage(
                "com.pipelinecrm.domainx", "com/pipelinecrm/domainx/X", "m", "()V", 1, 1, 0);
        CrapReport report = evaluator.evaluate(List.of(sibling), 6.0, List.of("com.pipelinecrm.domain"));
        assertThat(report.checkedMethodCount()).isEqualTo(0);
    }

    @Test
    void rejectsNullInputs() {
        assertThatThrownBy(() -> evaluator.evaluate(null, 6.0, List.of("p")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.evaluate(List.of(), 6.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluator.evaluate(List.of(), 6.0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reportRejectsInvalidConstruction() {
        assertThatThrownBy(() -> new CrapReport(-1, 6.0, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrapReport(0, -1.0, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrapReport(0, 6.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrapViolation(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrapViolation(MethodCoverageTest.sample("m", 1, 1, 0), -1.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
