import type { components } from './schema';

/** Types of the API, generated from the versioned contract (docs/api/openapi.json): `npm run api:generate`. */
export type AccountResponse = components['schemas']['AccountResponse'];
export type AccountType = AccountResponse['type'];
export type PagedAccounts = components['schemas']['PagedResponseAccountResponse'];
export type MeResponse = components['schemas']['MeResponse'];
export type JournalEntry = components['schemas']['JournalEntryResponse'];
export type JournalEntryStatus = JournalEntry['status'];
export type PagedJournalEntries = components['schemas']['PagedResponseJournalEntryResponse'];
export type TrialBalance = components['schemas']['TrialBalanceResponse'];
export type TrialBalanceLine = components['schemas']['TrialBalanceLineResponse'];
/** Optional filters of the accounts search. */
export interface AccountFilters {
  readonly name?: string;
  readonly type?: AccountType;
  readonly active?: boolean;
}
/** Optional filters of the journal entries search. Dates are ISO instants. */
export interface EntryFilters {
  readonly status?: JournalEntryStatus;
  readonly createdFrom?: string;
  readonly createdTo?: string;
}
/** Trial balance lines of one account type, with their subtotals (a view of the report, not an API type). */
export interface BalanceGroup {
  readonly type: AccountType;
  readonly lines: readonly TrialBalanceLine[];
  readonly totalDebits: string;
  readonly totalCredits: string;
}
export type Advice = components['schemas']['AdviceResponse'];
export type Recommendation = components['schemas']['RecommendationResponse'];
export type Severity = 'CRITICAL' | 'WARNING' | 'INFO';
export type AdviceCategory =
  'LIQUIDITY' | 'PROFITABILITY' | 'RISK' | 'COMPLIANCE' | 'GROWTH' | 'GENERAL';
export type CreateAccountRequest = components['schemas']['CreateAccountRequest'];
export type UpdateAccountRequest = components['schemas']['UpdateAccountRequest'];
export type RecordJournalEntryRequest = components['schemas']['RecordJournalEntryRequest'];
export type JournalEntryLineRequest = components['schemas']['JournalEntryLineRequest'];
export type EntryType = JournalEntryLineRequest['entryType'];
