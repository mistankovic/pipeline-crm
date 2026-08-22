package com.pipelinecrm.adapter.web.security;

import com.pipelinecrm.adapter.web.error.ApiError;
import com.pipelinecrm.adapter.web.error.ApiExceptionHandler;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.IllegalStageTransition;
import com.pipelinecrm.domain.deal.InapplicableActivityHistory;
import com.pipelinecrm.domain.identity.DealId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The 500 path in particular: it is the one response where the caller must learn nothing, and
 * nothing else in the suite would notice if a future edit put the exception's own message into
 * the body. See docs/reviews/stage-5-review.md, finding F-5.6.
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void a_translated_failure_keeps_its_name_and_message() {
        ResponseEntity<ApiError> response =
                handler.refused(new IllegalStageTransition(DealStage.LEAD, DealStage.CLOSED_WON));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("IllegalStageTransition");
        assertThat(response.getBody().message()).contains("LEAD", "CLOSED_WON");
    }

    @Test
    void a_server_fault_is_500() {
        ResponseEntity<ApiError> response =
                handler.refused(new InapplicableActivityHistory(DealId.of(UUID.randomUUID())));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void a_server_fault_tells_the_caller_nothing_about_what_went_wrong() {
        DealId deal = DealId.of(UUID.randomUUID());

        ResponseEntity<ApiError> response = handler.refused(new InapplicableActivityHistory(deal));

        assertThat(response.getBody().error()).isEqualTo("InternalError");
        assertThat(response.getBody().message()).isEqualTo("the request could not be completed");
        assertThat(response.getBody().message()).doesNotContain(deal.value().toString());
    }
}
