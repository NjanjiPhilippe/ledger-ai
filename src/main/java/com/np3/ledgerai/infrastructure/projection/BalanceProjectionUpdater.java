package com.np3.ledgerai.infrastructure.projection;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps balance_projection in step with the journal.
 *
 * <p>It runs inside the transaction that posts the entry (MANDATORY), not after it: the entry is posted if and
 * only if the projection is updated, so a failure here rolls the posting back instead of leaving the projection
 * silently behind. Row locks (SELECT ... FOR UPDATE, taken in account id order) serialise concurrent postings on
 * the same account, so no update is lost. A row that does not exist yet is created; two concurrent first
 * postings on a brand-new account can collide on the primary key, in which case one posting fails cleanly and
 * can simply be retried.
 */
@Component
public class BalanceProjectionUpdater {

    private final BalanceProjectionJpaRepository repository;

    public BalanceProjectionUpdater(BalanceProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onJournalEntryPosted(JournalEntryPostedEvent event) {
        UUID tenantId = event.tenantId().value();

        // One delta per account, in account id order (the same order the locks are taken in).
        Map<UUID, Delta> deltas = new TreeMap<>();
        for (TransactionLine line : event.posted().lines()) {
            deltas.computeIfAbsent(line.accountId().value(), id -> new Delta()).add(line);
        }

        Map<UUID, BalanceProjectionEntity> locked = repository.findAllForUpdate(tenantId, deltas.keySet()).stream()
                .collect(Collectors.toMap(BalanceProjectionEntity::getAccountId, Function.identity()));

        deltas.forEach((accountId, delta) -> {
            BalanceProjectionEntity projection = locked.getOrDefault(accountId,
                    new BalanceProjectionEntity(tenantId, accountId, BigDecimal.ZERO, BigDecimal.ZERO));
            projection.setTotalDebits(projection.getTotalDebits().add(delta.debits));
            projection.setTotalCredits(projection.getTotalCredits().add(delta.credits));
            repository.save(projection);
        });
    }

    private static final class Delta {
        private BigDecimal debits = BigDecimal.ZERO;
        private BigDecimal credits = BigDecimal.ZERO;

        void add(TransactionLine line) {
            if (line.isDebit()) {
                debits = debits.add(line.amount().amount());
            } else {
                credits = credits.add(line.amount().amount());
            }
        }
    }
}
