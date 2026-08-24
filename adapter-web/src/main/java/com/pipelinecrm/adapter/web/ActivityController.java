package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.application.port.in.RecordActivityUseCase;
import com.pipelinecrm.domain.activity.ActivityType;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final RecordActivityUseCase recordActivity;

    public ActivityController(RecordActivityUseCase recordActivity) {
        this.recordActivity = recordActivity;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.IdResponse create(Authentication authentication, @Valid @RequestBody ApiDtos.ActivityRequest request) {
        DealId dealId = request.dealId() == null ? null : DealId.parse(request.dealId());
        ContactId contactId = request.contactId() == null ? null : ContactId.parse(request.contactId());
        return new ApiDtos.IdResponse(recordActivity
                .execute(new RecordActivityUseCase.Command(
                        Actors.require(authentication),
                        ActivityType.valueOf(request.type()),
                        request.body(),
                        dealId,
                        contactId))
                .toString());
    }
}
