package com.pipelinecrm.adapter.web.rest;

import com.pipelinecrm.application.port.in.CreateCompany;
import com.pipelinecrm.application.port.in.ListCompanies;
import com.pipelinecrm.application.port.in.RenameCompany;
import com.pipelinecrm.application.view.CompanyView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Companies. */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final int LONGEST_NAME = 200;

    private final ListCompanies listing;
    private final CreateCompany creation;
    private final RenameCompany renaming;

    public CompanyController(ListCompanies listing, CreateCompany creation, RenameCompany renaming) {
        this.listing = listing;
        this.creation = creation;
        this.renaming = renaming;
    }

    @GetMapping
    public List<CompanyView> all() {
        return listing.handle();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyView create(@Valid @RequestBody NewCompanyRequest request) {
        return creation.handle(new CreateCompany.NewCompany(request.name()));
    }

    @PatchMapping("/{id}")
    public CompanyView rename(@PathVariable("id") UUID companyId,
                              @Valid @RequestBody RenameCompanyRequest request) {
        return renaming.handle(companyId, request.name());
    }

    /** What a caller sends to rename a company. */
    public record RenameCompanyRequest(@NotBlank @Size(max = LONGEST_NAME) String name) {
    }

    /** What a caller sends to add a company. */
    public record NewCompanyRequest(@NotBlank @Size(max = LONGEST_NAME) String name) {
    }
}
