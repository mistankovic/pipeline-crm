package com.pipelinecrm.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pipelinecrm.application.port.in.CreateCompanyUseCase;
import com.pipelinecrm.application.port.in.ListCompaniesUseCase;
import com.pipelinecrm.application.port.in.UpdateCompanyUseCase;
import com.pipelinecrm.domain.company.Company;
import com.pipelinecrm.domain.company.CompanyName;
import com.pipelinecrm.domain.identity.CompanyId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {

    @Mock
    private CreateCompanyUseCase createCompany;

    @Mock
    private UpdateCompanyUseCase updateCompany;

    @Mock
    private ListCompaniesUseCase listCompanies;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CompanyController(createCompany, updateCompany, listCompanies))
                .build();
    }

    @Test
    void listsAndCreatesAndUpdatesCompanies() throws Exception {
        CompanyId id = CompanyId.generate();
        when(listCompanies.execute()).thenReturn(List.of(Company.create(id, CompanyName.of("Acme"))));
        when(createCompany.execute(any())).thenReturn(id);

        mvc.perform(get("/api/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Acme"));
        mvc.perform(post("/api/companies").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Globex\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(put("/api/companies/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Inc\"}"))
                .andExpect(status().isNoContent());
        verify(updateCompany).execute(new UpdateCompanyUseCase.Command(id, "Acme Inc"));
    }
}
