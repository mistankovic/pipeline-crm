package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.adapter.web.security.SignedInUser;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * What a caller may send about a deal.
 *
 * <p>The constraints here are about the shape of a request — present, numeric, three letters —
 * never about the business. "A deal cannot be won without a meeting" is nowhere in this file
 * and must never be: it belongs to the domain, which is the only place that can enforce it for
 * every caller rather than for the ones who use this API.
 */
public final class DealRequests {

    private static final String CURRENCY_CODE = "^[A-Za-z]{3}$";
    private static final int LONGEST_TITLE = 200;
    private static final String LOWEST_PROBABILITY = "0";
    private static final int HIGHEST_PROBABILITY = 100;

    private DealRequests() {
    }

    /** A new opportunity. The owner defaults to whoever is asking. */
    public record NewDealRequest(
            @NotBlank @Size(max = LONGEST_TITLE) String title,
            @NotNull UUID companyId,
            UUID ownerId,
            @NotNull @DecimalMin(LOWEST_PROBABILITY) BigDecimal value,
            @NotBlank @Pattern(regexp = CURRENCY_CODE) String currency,
            @Min(0) @Max(HIGHEST_PROBABILITY) int probability) {

        public UUID ownerOr(SignedInUser caller) {
            return Optional.ofNullable(ownerId).orElseGet(() -> caller.id().value());
        }
    }

    /** Where to move a deal. The set of legal targets is the domain's business, not this one's. */
    public record StageRequest(@NotBlank String stage) {
    }

    /** What a deal is now worth. */
    public record ValueRequest(
            @NotNull @DecimalMin(LOWEST_PROBABILITY) BigDecimal amount,
            @NotBlank @Pattern(regexp = CURRENCY_CODE) String currency) {
    }

    /** How likely a deal now is. */
    public record ProbabilityRequest(@Min(0) @Max(HIGHEST_PROBABILITY) int probability) {
    }
}
