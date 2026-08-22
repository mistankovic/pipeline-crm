package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.security.SignedInUser;
import com.pipelinecrm.application.port.in.ChangeDealStage;
import com.pipelinecrm.application.port.in.CreateDeal;
import com.pipelinecrm.application.port.in.RepriceDeal;
import com.pipelinecrm.application.port.in.ReweightDeal;
import com.pipelinecrm.application.port.in.ViewDeal;
import com.pipelinecrm.application.port.in.ViewPipeline;
import com.pipelinecrm.application.view.DealView;
import com.pipelinecrm.domain.deal.DealStage;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deals.
 *
 * <p>Every method here reads the same way: take what HTTP delivered, name who is asking, call
 * one port, return what it gave back. There is no {@code if} in this class about a deal.
 * Whether a move is legal, who may make it, and what closing does are decided by the domain,
 * and the answers reach the caller as status codes through
 * {@code ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/deals")
public class DealController {

    private final ViewPipeline pipeline;
    private final ViewDeal detail;
    private final CreateDeal creation;
    private final DealChanges changes;

    public DealController(ViewPipeline pipeline, ViewDeal detail, CreateDeal creation, DealChanges changes) {
        this.pipeline = pipeline;
        this.detail = detail;
        this.creation = creation;
        this.changes = changes;
    }

    @GetMapping
    public List<DealView> board(@RequestParam(name = "ownerId", required = false) UUID ownerId,
                                @AuthenticationPrincipal SignedInUser caller) {
        UUID asker = caller.id().value();
        return Optional.ofNullable(ownerId)
                .map(owner -> pipeline.ownedBy(owner, asker))
                .orElseGet(() -> pipeline.everything(asker));
    }

    @GetMapping("/{id}")
    public ViewDeal.DealDetail one(@PathVariable("id") UUID dealId,
                                   @AuthenticationPrincipal SignedInUser caller) {
        return detail.handle(dealId, caller.id().value());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DealView create(@Valid @RequestBody DealRequests.NewDealRequest request,
                           @AuthenticationPrincipal SignedInUser caller) {
        UUID creator = caller.id().value();
        UUID owner = Optional.ofNullable(request.ownerId()).orElse(creator);
        return creation.handle(new CreateDeal.NewDeal(request.title(), request.companyId(),
                owner, creator, request.value(), request.currency(), request.probability()));
    }

    @PatchMapping("/{id}/stage")
    public DealView moveTo(@PathVariable("id") UUID dealId,
                           @Valid @RequestBody DealRequests.StageRequest request,
                           @AuthenticationPrincipal SignedInUser caller) {
        return changes.stage().handle(new ChangeDealStage.StageChange(
                dealId, RequestedEnum.of(DealStage.class, request.stage(), "stage"), caller.id().value()));
    }

    @PatchMapping("/{id}/value")
    public DealView reprice(@PathVariable("id") UUID dealId,
                            @Valid @RequestBody DealRequests.ValueRequest request,
                            @AuthenticationPrincipal SignedInUser caller) {
        return changes.price().handle(new RepriceDeal.Repricing(
                dealId, caller.id().value(), request.amount(), request.currency()));
    }

    @PatchMapping("/{id}/probability")
    public DealView reweight(@PathVariable("id") UUID dealId,
                             @Valid @RequestBody DealRequests.ProbabilityRequest request,
                             @AuthenticationPrincipal SignedInUser caller) {
        return changes.weight().handle(new ReweightDeal.Reweighting(
                dealId, caller.id().value(), request.probability()));
    }
}
