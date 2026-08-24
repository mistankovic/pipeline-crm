package com.pipelinecrm.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class ApiDtos {

    private ApiDtos() {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(String token, String userId) {}

    public record NameRequest(@NotBlank String name) {}

    public record CompanyResponse(String id, String name) {}

    public record ContactRequest(@NotBlank String companyId, @NotBlank String name, String email) {}

    public record ContactResponse(String id, String companyId, String name, String email) {}

    public record DealWriteRequest(
            @NotBlank String companyId,
            @NotBlank String ownerId,
            @NotBlank String title,
            @NotBlank String amount,
            @NotBlank String currency,
            @NotNull Integer probability) {}

    public record DealUpdateRequest(
            @NotBlank String title, @NotBlank String amount, @NotBlank String currency, @NotNull Integer probability) {}

    public record StageRequest(@NotBlank String stage) {}

    public record DealResponse(
            String id,
            String companyId,
            String ownerId,
            String title,
            String amount,
            String currency,
            int probability,
            String stage,
            java.util.List<ActivityResponse> activities) {}

    public record ActivityRequest(
            @NotBlank String type, @NotBlank String body, String dealId, String contactId) {}

    public record ActivityResponse(
            String id, String type, String body, String dealId, String contactId, String createdBy, String createdAt) {}

    public record ForecastResponse(String groupBy, String key, String amount, String currency) {}

    public record UserResponse(String id, String email, String name, String role) {}

    public record IdResponse(String id) {}
}
