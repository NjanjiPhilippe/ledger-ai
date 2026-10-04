package com.np3.ledgerai.web.error;

import com.np3.ledgerai.application.advisor.useCase.GenerateAdviceUseCase;
import com.np3.ledgerai.application.advisor.useCase.GenerateLedgerAdviceUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.PostJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.RecordJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.ReverseJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.query.GetJournalEntryQuery;
import com.np3.ledgerai.application.journalEntry.query.SearchJournalEntriesQuery;
import com.np3.ledgerai.domain.exception.AiAdvisorException;
import com.np3.ledgerai.domain.exception.NoActivityToAnalyzeException;
import com.np3.ledgerai.web.controller.AdvisorController;
import com.np3.ledgerai.web.controller.JournalEntryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What the frontend sees when something goes wrong: a precise status and a readable message. */
@WebMvcTest({JournalEntryController.class, AdvisorController.class})
@AutoConfigureMockMvc(addFilters = false)
class DomainExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @MockitoBean
    private GenerateAdviceUseCase generateAdviceUseCase;
    @MockitoBean
    private GenerateLedgerAdviceUseCase generateLedgerAdviceUseCase;
    @MockitoBean
    private Clock clock;

    @Test
    void aMalformedJsonBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/journal-entries").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void anUnknownEnumValueInTheBodyIsABadRequest() throws Exception {
        String body = """
                {"description":"x","currencyCode":"XAF","lines":[
                  {"accountId":"11111111-1111-1111-1111-111111111111","amount":10,"entryType":"SIDEWAYS"},
                  {"accountId":"22222222-2222-2222-2222-222222222222","amount":10,"entryType":"CREDIT"}]}""";

        mockMvc.perform(post("/api/v1/journal-entries").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anInvalidQueryParameterIsABadRequestThatNamesTheParameter() throws Exception {
        mockMvc.perform(get("/api/v1/journal-entries").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    void anInvalidPathIdIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/journal-entries/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anAdvisorProviderFailureIsABadGateway() throws Exception {
        when(generateLedgerAdviceUseCase.execute()).thenThrow(new AiAdvisorException("Échec de l'appel à Anthropic"));

        mockMvc.perform(post("/api/v1/advisor/analyze-ledger"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Échec de l'appel à Anthropic"));
    }

    @Test
    void askingForAdviceOnAnEmptyLedgerIsAConflict() throws Exception {
        when(generateLedgerAdviceUseCase.execute()).thenThrow(new NoActivityToAnalyzeException());

        mockMvc.perform(post("/api/v1/advisor/analyze-ledger"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No posted activity to analyze yet"));
    }
}
