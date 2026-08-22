package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.error.MalformedRequest;
import com.pipelinecrm.adapter.web.security.SignedInUser;
import com.pipelinecrm.application.port.in.LogActivity;
import com.pipelinecrm.application.view.ActivityView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Recording what happened. */
@RestController
public class ActivityController {

    private static final int LONGEST_SUMMARY = 2000;

    private final LogActivity logging;

    public ActivityController(LogActivity logging) {
        this.logging = logging;
    }

    @PostMapping("/api/activities")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityView log(@Valid @RequestBody NewActivityRequest request,
                            @AuthenticationPrincipal SignedInUser caller) {
        return logging.handle(new LogActivity.NewActivity(
                request.about(), request.type(), request.summary(), caller.id().value()));
    }

    /**
     * What a caller sends to record an activity. JSON has no sealed types, so both subject
     * fields arrive nullable and are collapsed into the port's sealed choice here — at the
     * boundary, once, rather than left for the use case to untangle.
     */
    public record NewActivityRequest(
            UUID dealId,
            UUID contactId,
            @NotBlank String type,
            @NotBlank @Size(max = LONGEST_SUMMARY) String summary) {

        public LogActivity.About about() {
            if (dealId != null && contactId == null) {
                return new LogActivity.AboutDeal(dealId);
            }
            if (contactId != null && dealId == null) {
                return new LogActivity.AboutContact(contactId);
            }
            throw new MalformedRequest("an activity is about exactly one of dealId and contactId");
        }
    }
}
