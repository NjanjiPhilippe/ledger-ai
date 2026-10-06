package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.account.command.useCase.CreateAccountUseCase;
import com.np3.ledgerai.application.account.command.useCase.UpdateAccountUseCase;
import com.np3.ledgerai.application.account.query.GetAccountBalanceQuery;
import com.np3.ledgerai.application.account.query.GetAccountQuery;
import com.np3.ledgerai.application.account.query.SearchAccountsQuery;
import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.web.dto.account.CreateAccountRequest;
import com.np3.ledgerai.web.dto.account.UpdateAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * NOTE: security is switched off for this slice (addFilters = false). @PreAuthorize
 * lives on the use cases, which are @MockitoBean here, so real method security never
 * runs in this test anyway -- this focuses purely on request mapping, validation,
 * status codes and DTO shape.
 */
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;
    @MockitoBean
    private UpdateAccountUseCase updateAccountUseCase;
    @MockitoBean
    private GetAccountQuery getAccountQuery;
    @MockitoBean
    private SearchAccountsQuery searchAccountsQuery;
    @MockitoBean
    private GetAccountBalanceQuery getAccountBalanceQuery;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
    }

    @Test
    void createReturns201WithTheCreatedAccount() throws Exception {
        when(createAccountUseCase.execute(any())).thenReturn(account);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccountRequest("Cash", AccountType.ASSET, "XAF"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cash"))
                .andExpect(jsonPath("$.currencyCode").value("XAF"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createReturns400WithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccountRequest("  ", null, "notacurrency"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void updateReturns200WithTheUpdatedAccount() throws Exception {
        when(updateAccountUseCase.execute(any())).thenReturn(account);

        mockMvc.perform(put("/api/v1/accounts/{id}", account.id().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountRequest("Cash", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cash"));
    }

    @Test
    void getReturns200WhenTheAccountExists() throws Exception {
        when(getAccountQuery.execute(account.id())).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/v1/accounts/{id}", account.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.id().value().toString()));
    }

    @Test
    void getReturns404WhenTheAccountDoesNotExist() throws Exception {
        AccountId missingId = AccountId.generate();
        when(getAccountQuery.execute(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/accounts/{id}", missingId.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturns200WithAPagedResponse() throws Exception {
        when(searchAccountsQuery.execute(any(), any()))
                .thenReturn(new PageResult<>(List.of(account), 0, 20, 1));

        mockMvc.perform(get("/api/v1/accounts").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Cash"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void balanceReturns200WithTheComputedAmount() throws Exception {
        when(getAccountBalanceQuery.execute(account.id())).thenReturn(Money.of(BigDecimal.valueOf(180), XAF));

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", account.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(180))
                .andExpect(jsonPath("$.currencyCode").value("XAF"));
    }

    @Test
    void balanceReturns404WhenTheAccountDoesNotExist() throws Exception {
        AccountId missingId = AccountId.generate();
        when(getAccountBalanceQuery.execute(missingId)).thenThrow(new AccountNotFoundException(missingId));

        mockMvc.perform(get("/api/v1/accounts/{id}/balance", missingId.value()))
                .andExpect(status().isNotFound());
    }
}