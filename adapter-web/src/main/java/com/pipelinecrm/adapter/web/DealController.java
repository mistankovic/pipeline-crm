package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.adapter.web.dto.Responses;
import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.CreateDealUseCase;
import com.pipelinecrm.application.port.in.ListDealsUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.port.in.ViewDealUseCase;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.deal.Money;
import com.pipelinecrm.domain.deal.Probability;
import com.pipelinecrm.domain.identity.CompanyId;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final CreateDealUseCase createDeal;
    private final UpdateDealUseCase updateDeal;
    private final ListDealsUseCase listDeals;
    private final ViewDealUseCase viewDeal;
    private final ChangeDealStageUseCase changeStage;

    public DealController(
            CreateDealUseCase createDeal,
            UpdateDealUseCase updateDeal,
            ListDealsUseCase listDeals,
            ViewDealUseCase viewDeal,
            ChangeDealStageUseCase changeStage) {
        this.createDeal = createDeal;
        this.updateDeal = updateDeal;
        this.listDeals = listDeals;
        this.viewDeal = viewDeal;
        this.changeStage = changeStage;
    }

    @GetMapping
    public List<ApiDtos.DealResponse> list(
            @RequestParam(required = false) String stage, @RequestParam(required = false) String ownerId) {
        DealStage dealStage = stage == null ? null : DealStage.valueOf(stage);
        UserId owner = ownerId == null ? null : UserId.parse(ownerId);
        return Responses.deals(listDeals.execute(new ListDealsUseCase.Filter(dealStage, owner)));
    }

    @GetMapping("/{id}")
    public ApiDtos.DealResponse view(@PathVariable String id) {
        return Responses.deal(viewDeal.execute(DealId.parse(id)).deal());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.IdResponse create(Authentication authentication, @Valid @RequestBody ApiDtos.DealWriteRequest request) {
        DealId id = createDeal.execute(new CreateDealUseCase.Command(
                Actors.require(authentication),
                CompanyId.parse(request.companyId()),
                UserId.parse(request.ownerId()),
                request.title(),
                Money.of(request.amount(), request.currency()),
                Probability.of(request.probability())));
        return new ApiDtos.IdResponse(id.toString());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.DealUpdateRequest request) {
        updateDeal.execute(new UpdateDealUseCase.Command(
                Actors.require(authentication),
                DealId.parse(id),
                request.title(),
                Money.of(request.amount(), request.currency()),
                Probability.of(request.probability())));
    }

    @PostMapping("/{id}/stage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeStage(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody ApiDtos.StageRequest request) {
        changeStage.execute(new ChangeDealStageUseCase.Command(
                Actors.require(authentication), DealId.parse(id), DealStage.valueOf(request.stage())));
    }
}
