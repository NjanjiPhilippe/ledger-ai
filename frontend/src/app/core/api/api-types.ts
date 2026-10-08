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
