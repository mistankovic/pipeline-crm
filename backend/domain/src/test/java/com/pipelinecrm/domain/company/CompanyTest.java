package com.pipelinecrm.domain.company;

import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    private static final CompanyId AN_ID = CompanyId.of(UUID.randomUUID());

    @Test
    void trims_its_name() {
        assertThat(new Company(AN_ID, "  Acme  ").name()).isEqualTo("Acme");
    }

    @Test
    void refuses_to_exist_without_an_id() {
        assertThatThrownBy(() -> new Company(null, "Acme")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void refuses_to_exist_without_a_name() {
        assertThatThrownBy(() -> new Company(AN_ID, "")).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void keeps_its_identity_when_renamed() {
        Company renamed = new Company(AN_ID, "Acme").renamedTo("Acme Holdings");

        assertThat(renamed.id()).isEqualTo(AN_ID);
        assertThat(renamed.name()).isEqualTo("Acme Holdings");
    }

    @Test
    void applies_the_same_name_rules_when_renamed() {
        Company acme = new Company(AN_ID, "Acme");

        assertThat(acme.renamedTo("  Acme Holdings  ").name()).isEqualTo("Acme Holdings");
        assertThatThrownBy(() -> acme.renamedTo("  ")).isInstanceOf(InvariantViolation.class);
        assertThatThrownBy(() -> acme.renamedTo(null)).isInstanceOf(InvariantViolation.class);
    }

    @Test
    void leaves_the_original_alone_when_renamed() {
        Company acme = new Company(AN_ID, "Acme");

        acme.renamedTo("Acme Holdings");

        assertThat(acme.name()).isEqualTo("Acme");
    }
}
