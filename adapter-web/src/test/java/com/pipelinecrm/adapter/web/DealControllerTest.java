package com.pipelinecrm.adapter.web;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pipelinecrm.application.port.in.ChangeDealStageUseCase;
import com.pipelinecrm.application.port.in.CreateDealUseCase;
import com.pipelinecrm.application.port.in.ListDealsUseCase;
import com.pipelinecrm.application.port.in.UpdateDealUseCase;
import com.pipelinecrm.application.port.in.ViewDealUseCase;
import com.pipelinecrm.domain.deal.DealStage;
import com.pipelinecrm.domain.identity.DealId;
import com.pipelinecrm.domain.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DealControllerTest {

    @Mock
    private CreateDealUseCase createDeal;

    @Mock
    private UpdateDealUseCase updateDeal;

    @Mock
    private ListDealsUseCase listDeals;

    @Mock
    private ViewDealUseCase viewDeal;

    @Mock
    private ChangeDealStageUseCase changeStage;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new DealController(createDeal, updateDeal, listDeals, viewDeal, changeStage))
                .build();
    }

    @Test
    void stageChangeDelegatesToUseCase() throws Exception {
        UserId actor = UserId.generate();
        DealId dealId = DealId.generate();
        var authentication = new UsernamePasswordAuthenticationToken(actor.toString(), null);
        mvc.perform(post("/api/deals/" + dealId + "/stage")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"QUALIFIED\"}"))
                .andExpect(status().isNoContent());
        verify(changeStage).execute(new ChangeDealStageUseCase.Command(actor, dealId, DealStage.QUALIFIED));
    }
}
