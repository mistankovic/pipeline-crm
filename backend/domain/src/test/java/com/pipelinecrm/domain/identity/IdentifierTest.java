package com.pipelinecrm.domain.identity;

import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The five identifier types are deliberately identical in shape, so they are pinned down
 * once rather than five times.
 */
class IdentifierTest {

    static Stream<Arguments> identifierTypes() {
        return Stream.of(
                Arguments.of("UserId", (Function<UUID, Identifier>) UserId::of,
                        (Function<String, Identifier>) UserId::fromString),
                Arguments.of("CompanyId", (Function<UUID, Identifier>) CompanyId::of,
                        (Function<String, Identifier>) CompanyId::fromString),
                Arguments.of("ContactId", (Function<UUID, Identifier>) ContactId::of,
                        (Function<String, Identifier>) ContactId::fromString),
                Arguments.of("DealId", (Function<UUID, Identifier>) DealId::of,
                        (Function<String, Identifier>) DealId::fromString),
                Arguments.of("ActivityId", (Function<UUID, Identifier>) ActivityId::of,
                        (Function<String, Identifier>) ActivityId::fromString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierTypes")
    void wraps_the_uuid_it_is_given(String name, Function<UUID, Identifier> fromUuid) {
        UUID uuid = UUID.randomUUID();

        assertThat(fromUuid.apply(uuid).value()).isEqualTo(uuid);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierTypes")
    void reads_the_same_identity_back_from_its_text_form(
            String name, Function<UUID, Identifier> fromUuid, Function<String, Identifier> fromText) {
        UUID uuid = UUID.randomUUID();

        assertThat(fromText.apply(uuid.toString())).isEqualTo(fromUuid.apply(uuid));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierTypes")
    void refuses_to_exist_without_a_uuid(String name, Function<UUID, Identifier> fromUuid) {
        assertThatThrownBy(() -> fromUuid.apply(null))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("id is required");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierTypes")
    void refuses_to_exist_without_text(
            String name, Function<UUID, Identifier> fromUuid, Function<String, Identifier> fromText) {
        assertThatThrownBy(() -> fromText.apply(" "))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("must not be blank");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("identifierTypes")
    void refuses_text_that_is_not_a_uuid(
            String name, Function<UUID, Identifier> fromUuid, Function<String, Identifier> fromText) {
        assertThatThrownBy(() -> fromText.apply("not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
    }
}
