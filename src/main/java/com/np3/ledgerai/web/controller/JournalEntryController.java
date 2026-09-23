package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.journalEntry.query.GetJournalEntryQuery;
import com.np3.ledgerai.application.journalEntry.command.useCase.PostJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.RecordJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.ReverseJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.query.SearchJournalEntriesQuery;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.web.dto.PagedResponse;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryResponse;
import com.np3.ledgerai.web.dto.journalEntry.RecordJournalEntryRequest;
import com.np3.ledgerai.web.mapper.JournalEntryWebMapper;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/journal-entries")
public class JournalEntryController {

    private final RecordJournalEntryUseCase recordJournalEntryUseCase;
    private final GetJournalEntryQuery getJournalEntryQuery;
    private final SearchJournalEntriesQuery searchJournalEntriesQuery;
    private final PostJournalEntryUseCase postJournalEntryUseCase;
    private final ReverseJournalEntryUseCase reverseJournalEntryUseCase;

    public JournalEntryController(RecordJournalEntryUseCase recordJournalEntryUseCase,
                                  GetJournalEntryQuery getJournalEntryQuery,
                                  SearchJournalEntriesQuery searchJournalEntriesQuery,
                                  PostJournalEntryUseCase postJournalEntryUseCase,
                                  ReverseJournalEntryUseCase reverseJournalEntryUseCase) {
        this.recordJournalEntryUseCase = recordJournalEntryUseCase;
        this.getJournalEntryQuery = getJournalEntryQuery;
        this.searchJournalEntriesQuery = searchJournalEntriesQuery;
        this.postJournalEntryUseCase = postJournalEntryUseCase;
        this.reverseJournalEntryUseCase = reverseJournalEntryUseCase;
    }

    @PostMapping
    public ResponseEntity<JournalEntryResponse> record(@Valid @RequestBody RecordJournalEntryRequest request) {
        var journalEntry = recordJournalEntryUseCase.execute(JournalEntryWebMapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalEntryWebMapper.toResponse(journalEntry));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> get(@PathVariable UUID id) {
        return getJournalEntryQuery.execute(JournalEntryId.of(id))
                .map(JournalEntryWebMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PagedResponse<JournalEntryResponse>> search(
            @RequestParam(required = false) JournalEntryStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var criteria = new JournalEntrySearchCriteria(status, createdFrom, createdTo);
        var result = searchJournalEntriesQuery.execute(criteria, new PageRequest(page, size));
        return ResponseEntity.ok(PagedResponse.from(result, JournalEntryWebMapper::toResponse));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<JournalEntryResponse> post(@PathVariable UUID id) {
        var journalEntry = postJournalEntryUseCase.execute(JournalEntryId.of(id));
        return ResponseEntity.ok(JournalEntryWebMapper.toResponse(journalEntry));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<JournalEntryResponse> reverse(@PathVariable UUID id) {
        var reversal = reverseJournalEntryUseCase.execute(JournalEntryId.of(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(JournalEntryWebMapper.toResponse(reversal));
    }
}