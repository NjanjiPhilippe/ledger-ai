package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.advisor.useCase.GenerateAdviceUseCase;
import com.np3.ledgerai.application.advisor.useCase.GenerateLedgerAdviceUseCase;
import com.np3.ledgerai.web.dto.advisor.AdviceResponse;
import com.np3.ledgerai.web.dto.advisor.AnalyzeSnapshotRequest;
import com.np3.ledgerai.web.mapper.AdvisorWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
public class AdvisorController {
    private final GenerateAdviceUseCase generateAdviceUseCase;
    private final GenerateLedgerAdviceUseCase generateLedgerAdviceUseCase;
    private final Clock clock;

    @Operation(summary = "Analyze a snapshot supplied by the caller",
            description = "Useful for what-if analysis: you provide the figures, the advisor comments them.")
    @PostMapping("/analyze")
    public ResponseEntity<AdviceResponse> analyze(@Valid @RequestBody AnalyzeSnapshotRequest request) {
        var snapshot = AdvisorWebMapper.toSnapshot(request, clock.instant());
        var advice = generateAdviceUseCase.execute(snapshot);
        return ResponseEntity.ok(AdvisorWebMapper.toResponse(advice));
    }

    @Operation(summary = "Analyze the current state of the ledger",
            description = "The server composes the snapshot from the trial balance (posted entries only), then asks the advisor.")
    @PostMapping("/analyze-ledger")
    public ResponseEntity<AdviceResponse> analyzeLedger() {
        var advice = generateLedgerAdviceUseCase.execute();
        return ResponseEntity.ok(AdvisorWebMapper.toResponse(advice));
    }
}