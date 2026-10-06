package com.np3.ledgerai.web.dto.journalEntry;

import com.np3.ledgerai.domain.valueobject.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineRequest( @NotNull(message = "Account id is required")
                                       UUID accountId,

                                       @NotNull(message = "Amount is required")
                                       @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
                                       @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, as a string (a JSON number is also accepted)")
                                       BigDecimal amount,

                                       @NotNull(message = "Entry type is required")
                                       EntryType entryType) {
}
