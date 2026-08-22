package com.pipelinecrm.adapter.persistence.mapping;

import com.pipelinecrm.adapter.persistence.row.ActivityRow;
import com.pipelinecrm.domain.shared.InvariantViolation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database forbids an activity about nothing, so no integration test can produce one. The
 * mapper defends against it anyway — for a hand-edited database, or a future migration that
 * relaxes the constraint — and a defence nobody has ever executed is not a defence.
 * See docs/reviews/stage-4-review.md, finding F-4.4.
 */
class ActivityMappingTest {

    @Test
    void refuses_a_row_that_is_about_neither_a_deal_nor_a_contact() {
        ActivityRow orphan = new ActivityRow(UUID.randomUUID(),
                new ActivityRow.ActivitySubjectColumns(null, null),
                new ActivityRow.ActivityContent("NOTE", "about nothing"),
                new ActivityRow.ActivityRecord(UUID.randomUUID(), Instant.parse("2026-03-01T09:00:00Z")));

        assertThatThrownBy(() -> ActivityMapping.toDomain(orphan))
                .isInstanceOf(InvariantViolation.class)
                .hasMessageContaining("about neither a deal nor a contact");
    }
}
