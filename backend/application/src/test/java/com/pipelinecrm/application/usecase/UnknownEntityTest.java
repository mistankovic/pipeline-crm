package com.pipelinecrm.application.usecase;

import com.pipelinecrm.application.error.UnknownEntity;
import com.pipelinecrm.domain.identity.ActivityId;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.Identifier;
import com.pipelinecrm.domain.identity.UserId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wording of "not found" is part of the contract: the acceptance scenarios and, from
 * Stage 5, the HTTP error bodies both read it.
 */
class UnknownEntityTest {

    static Stream<Arguments> identities() {
        UUID id = UUID.randomUUID();
        return Stream.of(
                Arguments.of(DealId.of(id), "no deal with id " + id),
                Arguments.of(UserId.of(id), "no user with id " + id),
                Arguments.of(CompanyId.of(id), "no company with id " + id),
                Arguments.of(ContactId.of(id), "no contact with id " + id),
                Arguments.of(ActivityId.of(id), "no activity with id " + id));
    }

    @ParameterizedTest
    @MethodSource("identities")
    void names_the_kind_of_thing_that_was_missing(Identifier id, String expected) {
        assertThat(new UnknownEntity(id)).hasMessage(expected);
    }
}
