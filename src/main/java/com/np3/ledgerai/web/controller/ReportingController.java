package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.application.reporting.query.GetTrialBalanceQuery;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceResponse;
import com.np3.ledgerai.web.mapper.TrialBalanceWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportingController {

    private final GetTrialBalanceQuery getTrialBalanceQuery;

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceResponse> trialBalance() {
        var trialBalance = getTrialBalanceQuery.execute();
        return ResponseEntity.ok(TrialBalanceWebMapper.toResponse(trialBalance));
    }
}
