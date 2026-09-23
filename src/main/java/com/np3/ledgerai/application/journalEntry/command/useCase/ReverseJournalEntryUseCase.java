package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ReverseJournalEntryUseCase {
    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ReverseJournalEntryUseCase(JournalEntryRepository journalEntryRepository, TenantContext tenantContext,
                                      CurrentUserProvider currentUserProvider, Clock clock) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public JournalEntry execute(JournalEntryId originalId) {
        JournalEntry original = journalEntryRepository.findById(tenantContext.currentTenantId(), originalId)
                .orElseThrow(() -> new JournalEntryNotFoundException(originalId));

        Instant now = Instant.now(clock);

        List<TransactionLine> reversedLines = original.lines().stream()
                .map(line -> new TransactionLine(line.accountId(), line.amount(), opposite(line.entryType())))
                .toList();

        JournalEntry reversal = JournalEntry.draftReversal(
                tenantContext.currentTenantId(),
                original.id(),
                reversedLines,
                "Reversal of: " + original.description(),
                now,
                currentUserProvider.currentUserId());

        reversal.post(now);
        JournalEntry savedReversal = journalEntryRepository.save(reversal);

        original.markReversed();
        journalEntryRepository.save(original);

        return savedReversal;
    }

    private static EntryType opposite(EntryType entryType) {
        return entryType == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;
    }
}
