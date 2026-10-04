package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.service.JournalEntryAccountsPolicy;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PostJournalEntryUseCase {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final TenantContext tenantContext;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public PostJournalEntryUseCase(JournalEntryRepository journalEntryRepository, AccountRepository accountRepository,
                                   TenantContext tenantContext, Clock clock, ApplicationEventPublisher eventPublisher) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
        this.tenantContext = tenantContext;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Transactional
    public JournalEntry execute(JournalEntryId id) {
        var tenantId = tenantContext.currentTenantId();
        JournalEntry journalEntry = journalEntryRepository.findById(tenantId, id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        // An account may have been deactivated between the draft and the posting. Non-drafts are left to
        // post(), which rejects them with a state error that is more useful than an account error.
        if (journalEntry.status() == JournalEntryStatus.DRAFT) {
            JournalEntryAccountsPolicy.requireUsableAccounts(journalEntry.lines(),
                    accountId -> accountRepository.findById(tenantId, accountId));
        }

        JournalEntryPosted event = journalEntry.post(Instant.now(clock));
        JournalEntry saved = journalEntryRepository.save(journalEntry);

        eventPublisher.publishEvent(new JournalEntryPostedEvent(saved.tenantId(), event));

        return saved;
    }
}
