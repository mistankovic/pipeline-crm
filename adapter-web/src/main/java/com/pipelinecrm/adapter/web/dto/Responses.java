package com.pipelinecrm.adapter.web.dto;

import com.pipelinecrm.application.port.in.ListUsersUseCase;
import com.pipelinecrm.domain.activity.Activity;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.contact.Contact;
import com.pipelinecrm.domain.deal.Deal;
import com.pipelinecrm.domain.forecast.ForecastBucket;
import com.pipelinecrm.domain.identity.ContactId;
import com.pipelinecrm.domain.identity.DealId;
import java.util.List;

public final class Responses {

    private Responses() {}

    public static ApiDtos.CompanyResponse company(Company company) {
        return new ApiDtos.CompanyResponse(company.id().toString(), company.name().value());
    }

    public static ApiDtos.ContactResponse contact(Contact contact) {
        String email = contact.email() == null ? null : contact.email().value();
        return new ApiDtos.ContactResponse(
                contact.id().toString(), contact.companyId().toString(), contact.name().value(), email);
    }

    public static ApiDtos.DealResponse deal(Deal deal) {
        return new ApiDtos.DealResponse(
                deal.id().toString(),
                deal.companyId().toString(),
                deal.ownerId().toString(),
                deal.title().value(),
                deal.value().amount().toPlainString(),
                deal.value().currency().getCurrencyCode(),
                deal.probability().percent(),
                deal.stage().name(),
                deal.activities().stream().map(Responses::activity).toList());
    }

    public static ApiDtos.ActivityResponse activity(Activity activity) {
        return new ApiDtos.ActivityResponse(
                activity.id().toString(),
                activity.type().name(),
                activity.body().value(),
                activity.target().dealId().map(DealId::toString).orElse(null),
                activity.target().contactId().map(ContactId::toString).orElse(null),
                activity.createdBy().toString(),
                activity.createdAt().toString());
    }

    public static ApiDtos.ForecastResponse forecast(String groupBy, ForecastBucket bucket) {
        String key = bucket.ownerId()
                .map(Object::toString)
                .orElseGet(() -> bucket.stage().map(Enum::name).orElse(""));
        return new ApiDtos.ForecastResponse(
                groupBy, key, bucket.total().amount().toPlainString(), bucket.total().currency().getCurrencyCode());
    }

    public static ApiDtos.UserResponse user(ListUsersUseCase.PublicUser user) {
        return new ApiDtos.UserResponse(user.id().toString(), user.email(), user.name(), user.role());
    }

    public static List<ApiDtos.CompanyResponse> companies(List<Company> companies) {
        return companies.stream().map(Responses::company).toList();
    }

    public static List<ApiDtos.ContactResponse> contacts(List<Contact> contacts) {
        return contacts.stream().map(Responses::contact).toList();
    }

    public static List<ApiDtos.DealResponse> deals(List<Deal> deals) {
        return deals.stream().map(Responses::deal).toList();
    }
}
