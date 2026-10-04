package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.reporting.query.GetTrialBalanceQuery;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Security switched off for this slice -- see the note in AccountControllerTest;
 * @PreAuthorize lives on the mocked GetTrialBalanceQuery, not the controller.
 */
@WebMvcTest(ReportingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReportingControllerTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTrialBalanceQuery getTrialBalanceQuery;

    @Test
    void trialBalanceReturns200WithTheReport() throws Exception {
        TrialBalanceLine line = new TrialBalanceLine(AccountId.generate(), "Cash", AccountType.ASSET,
                Money.of(BigDecimal.valueOf(100), XAF),
                Money.of(BigDecimal.valueOf(200), XAF),
                Money.of(BigDecimal.valueOf(100), XAF));

        TrialBalance trialBalance = new TrialBalance(Instant.now(), XAF, List.of(line),
                Money.of(BigDecimal.valueOf(200), XAF),
                Money.of(BigDecimal.valueOf(200), XAF));

        when(getTrialBalanceQuery.execute()).thenReturn(trialBalance);

        mockMvc.perform(get("/api/v1/reports/trial-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].accountName").value("Cash"))
                .andExpect(jsonPath("$.totalDebits").value(200))
                .andExpect(jsonPath("$.totalCredits").value(200))
                .andExpect(jsonPath("$.currencyCode").value("XAF"))
                .andExpect(jsonPath("$.balanced").value(true));
    }

    @Test
    void trialBalanceReturns200WithAnEmptyReportWhenThereAreNoAccountsYet() throws Exception {
        // Mirrors GetTrialBalanceQuery's own behavior: an accountless tenant still gets a
        // non-null currency (its DEFAULT_CURRENCY fallback), never a null one -- TrialBalance's
        // compact constructor forbids that outright.
        TrialBalance emptyTrialBalance = new TrialBalance(Instant.now(), XAF, List.of(),
                Money.of(BigDecimal.ZERO, XAF),
                Money.of(BigDecimal.ZERO, XAF));

        when(getTrialBalanceQuery.execute()).thenReturn(emptyTrialBalance);

        mockMvc.perform(get("/api/v1/reports/trial-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isEmpty())
                .andExpect(jsonPath("$.currencyCode").value("XAF"));
    }
}