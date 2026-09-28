package com.np3.ledgerai.infrastructure.projection;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Component
public class BalanceProjectionUpdater {

    private final BalanceProjectionJpaRepository repository;

    public BalanceProjectionUpdater(BalanceProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJournalEntryPosted(JournalEntryPostedEvent event) {
        var tenantId = event.tenantId().value();

        event.posted().lines().forEach(line -> {
            var accountId = line.accountId().value();
            var projection = repository.findById(new BalanceProjectionEntity.BalanceProjectionId(tenantId, accountId))
                    .orElseGet(() -> new BalanceProjectionEntity(tenantId, accountId, BigDecimal.ZERO, BigDecimal.ZERO));

            if (line.isDebit()) {
                projection.setTotalDebits(projection.getTotalDebits().add(line.amount().amount()));
            } else {
                projection.setTotalCredits(projection.getTotalCredits().add(line.amount().amount()));
            }

            repository.save(projection);
        });
    }
}
