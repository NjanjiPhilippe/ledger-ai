package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.journalEntry.command.useCase.PostJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.RecordJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.ReverseJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.query.GetJournalEntryQuery;
import com.np3.ledgerai.application.journalEntry.query.SearchJournalEntriesQuery;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryLineRequest;
import com.np3.ledgerai.web.dto.journalEntry.RecordJournalEntryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Security is switched off for this slice (addFilters = false) -- see the note in
 * AccountControllerTest for why that's fine given @PreAuthorize lives on the mocked
 * use cases, not the controller.
 */
@WebMvcTest(JournalEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class JournalEntryControllerTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecordJournalEntryUseCase recordJournalEntryUseCase;
    @MockitoBean
    private GetJournalEntryQuery getJournalEntryQuery;
    @MockitoBean
    private SearchJournalEntriesQuery searchJournalEntriesQuery;
    @MockitoBean
    private PostJournalEntryUseCase postJournalEntryUseCase;
    @MockitoBean
    private ReverseJournalEntryUseCase reverseJournalEntryUseCase;

    private JournalEntry entry;

    @BeforeEach
    void setUp() {
        entry = JournalEntry.draft(
                TENANT_ID,
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)),
                "Office supplies",
                Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));
    }

    @Test
    void recordReturns201WithTheRecordedEntry() throws Exception {
        when(recordJournalEntryUseCase.execute(any())).thenReturn(entry);

        RecordJournalEntryRequest request = new RecordJournalEntryRequest(
                "Office supplies", "XAF",
                List.of(
                        new JournalEntryLineRequest(UUID.randomUUID(), BigDecimal.valueOf(100), EntryType.DEBIT),
                        new JournalEntryLineRequest(UUID.randomUUID(), BigDecimal.valueOf(100), EntryType.CREDIT)));

        mockMvc.perform(post("/api/v1/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Office supplies"))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void recordReturns400WhenFewerThanTwoLinesAreGiven() throws Exception {
        RecordJournalEntryRequest request = new RecordJournalEntryRequest(
                "Invalid", "XAF",
                List.of(new JournalEntryLineRequest(UUID.randomUUID(), BigDecimal.valueOf(100), EntryType.DEBIT)));

        mockMvc.perform(post("/api/v1/journal-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getReturns200WhenTheEntryExists() throws Exception {
        when(getJournalEntryQuery.execute(entry.id())).thenReturn(Optional.of(entry));

        mockMvc.perform(get("/api/v1/journal-entries/{id}", entry.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Office supplies"));
    }

    @Test
    void getReturns404WhenTheEntryDoesNotExist() throws Exception {
        JournalEntryId missingId = JournalEntryId.generate();
        when(getJournalEntryQuery.execute(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/journal-entries/{id}", missingId.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturns200WithAPagedResponse() throws Exception {
        when(searchJournalEntriesQuery.execute(any(), any()))
                .thenReturn(new PageResult<>(List.of(entry), 0, 20, 1));

        mockMvc.perform(get("/api/v1/journal-entries").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Office supplies"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void postReturns200WithThePostedEntry() throws Exception {
        JournalEntry posted = JournalEntry.draft(TENANT_ID, entry.lines(), entry.description(),
                entry.createdAt(), entry.createdBy());
        posted.post(Instant.parse("2026-01-02T00:00:00Z"));
        when(postJournalEntryUseCase.execute(entry.id())).thenReturn(posted);

        mockMvc.perform(post("/api/v1/journal-entries/{id}/post", entry.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));
    }

    @Test
    void reverseReturns201WithTheReversalEntry() throws Exception {
        JournalEntry reversal = JournalEntry.draftReversal(TENANT_ID, entry.id(), entry.lines(),
                "Reversal of: Office supplies", Instant.parse("2026-01-03T00:00:00Z"), entry.createdBy());
        reversal.post(Instant.parse("2026-01-03T00:00:00Z"));
        when(reverseJournalEntryUseCase.execute(entry.id())).thenReturn(reversal);

        mockMvc.perform(post("/api/v1/journal-entries/{id}/reverse", entry.id().value()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reversalOfId").value(entry.id().value().toString()));
    }
}