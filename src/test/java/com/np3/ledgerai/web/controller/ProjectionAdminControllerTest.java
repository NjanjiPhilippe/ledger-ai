package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.reporting.command.RebuildBalanceProjectionResult;
import com.np3.ledgerai.application.reporting.command.useCase.RebuildBalanceProjectionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/* Security is off for this slice: @PreAuthorize("hasRole('ADMIN')") lives on the (mocked) use case. */
@WebMvcTest(ProjectionAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RebuildBalanceProjectionUseCase rebuildBalanceProjectionUseCase;

    @Test
    void rebuildReturnsHowManyAccountsWereRebuiltAndCorrected() throws Exception {
        when(rebuildBalanceProjectionUseCase.execute()).thenReturn(new RebuildBalanceProjectionResult(12, 2));

        mockMvc.perform(post("/api/v1/admin/projections/balances/rebuild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountsRebuilt").value(12))
                .andExpect(jsonPath("$.accountsCorrected").value(2));
    }
}
