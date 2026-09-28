package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.advisor.useCase.GenerateAdviceUseCase;
import com.np3.ledgerai.web.dto.advisor.AdviceResponse;
import com.np3.ledgerai.web.dto.advisor.AnalyzeSnapshotRequest;
import com.np3.ledgerai.web.mapper.AdvisorWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final GenerateAdviceUseCase generateAdviceUseCase;

    @PostMapping("/analyze")
    public ResponseEntity<AdviceResponse> analyze(@Valid @RequestBody AnalyzeSnapshotRequest request) {
        var snapshot = AdvisorWebMapper.toSnapshot(request);
        var advice = generateAdviceUseCase.execute(snapshot);
        return ResponseEntity.ok(AdvisorWebMapper.toResponse(advice));
    }
}