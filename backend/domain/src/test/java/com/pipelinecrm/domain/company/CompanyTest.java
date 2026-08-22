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
}
