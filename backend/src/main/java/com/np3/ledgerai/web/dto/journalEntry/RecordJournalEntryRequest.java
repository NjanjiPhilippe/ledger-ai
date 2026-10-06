package com.np3.ledgerai.web.dto.journalEntry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecordJournalEntryRequest(@NotBlank(message = "Description is required")
                                        String description,

                                        @NotBlank(message = "Currency code is required")
                                        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a 3-letter ISO code (e.g. XAF, USD)")
                                        String currencyCode,

                                        @NotEmpty(message = "At least two journal entry lines are required")
                                        @Size(min = 2, message = "At least two journal entry lines are required")
                                        List<@Valid JournalEntryLineRequest> lines) {
}
