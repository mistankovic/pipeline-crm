package com.pipelinecrm.adapter.web;

import com.pipelinecrm.adapter.web.dto.ApiDtos;
import com.pipelinecrm.adapter.web.dto.Responses;
import com.pipelinecrm.application.port.in.CreateCompanyUseCase;
import com.pipelinecrm.application.port.in.ListCompaniesUseCase;
import com.pipelinecrm.application.port.in.UpdateCompanyUseCase;
import com.pipelinecrm.domain.identity.CompanyId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CreateCompanyUseCase createCompany;
    private final UpdateCompanyUseCase updateCompany;
    private final ListCompaniesUseCase listCompanies;

    public CompanyController(
            CreateCompanyUseCase createCompany,
            UpdateCompanyUseCase updateCompany,
            ListCompaniesUseCase listCompanies) {
        this.createCompany = createCompany;
        this.updateCompany = updateCompany;
        this.listCompanies = listCompanies;
    }

    @GetMapping
    public List<ApiDtos.CompanyResponse> list() {
        return Responses.companies(listCompanies.execute());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.IdResponse create(@Valid @RequestBody ApiDtos.NameRequest request) {
        return new ApiDtos.IdResponse(createCompany.execute(new CreateCompanyUseCase.Command(request.name())).toString());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody ApiDtos.NameRequest request) {
        updateCompany.execute(new UpdateCompanyUseCase.Command(CompanyId.parse(id), request.name()));
    }
}
