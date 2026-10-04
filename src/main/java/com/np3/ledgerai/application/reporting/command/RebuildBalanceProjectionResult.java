package com.np3.ledgerai.application.reporting.command;

/**
 * @param accountsRebuilt   accounts that have posted activity in the rebuilt projection
 * @param accountsCorrected accounts whose projection row did not match the journal (the detected drift)
 */
public record RebuildBalanceProjectionResult(int accountsRebuilt, int accountsCorrected) {
}
