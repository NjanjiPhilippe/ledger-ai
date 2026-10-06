package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.reporting.command.useCase.RebuildBalanceProjectionUseCase;
import com.np3.ledgerai.web.dto.reporting.RebuildProjectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/projections")
@RequiredArgsConstructor
@Tag(name = "Administration", description = "Maintenance operations (admin only)")
public class ProjectionAdminController {

    private final RebuildBalanceProjectionUseCase rebuildBalanceProjectionUseCase;

    @Operation(summary = "Rebuild the balance projection from the journal",
            description = "Detects accounts whose projected balance drifted from the posted journal lines, "
                    + "repairs them, and reports how many were corrected.")
    @PostMapping("/balances/rebuild")
    public ResponseEntity<RebuildProjectionResponse> rebuildBalances() {
        var result = rebuildBalanceProjectionUseCase.execute();
        return ResponseEntity.ok(new RebuildProjectionResponse(result.accountsRebuilt(), result.accountsCorrected()));
    }
}
