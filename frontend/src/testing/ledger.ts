import {
  JournalEntry,
  PagedAccounts,
  PagedJournalEntries,
  TrialBalance,
} from '../app/core/api/api-types';
import { CASH } from './accounts';

export const SALES = {
  id: '33333333-3333-3333-3333-333333333333',
  name: 'Sales',
  type: 'REVENUE',
  currencyCode: 'XAF',
  active: true,
} as const;

export const ACCOUNTS: PagedAccounts = {
  content: [
    CASH,
    SALES,
    {
      ...SALES,
      id: '44444444-4444-4444-4444-444444444444',
      name: 'Rent',
      type: 'EXPENSE',
      active: false,
    },
  ],
  page: 0,
  size: 100,
  totalElements: 3,
  totalPages: 1,
};

export const TRIAL_BALANCE: TrialBalance = {
  balanced: true,
  currencyCode: 'XAF',
  generatedAt: '2026-10-08T10:00:00Z',
  totalDebits: '2450000.00',
  totalCredits: '2450000.00',
  lines: [
    {
      accountId: CASH.id,
      accountName: 'Cash',
      accountType: 'ASSET',
      totalDebits: '2450000.00',
      totalCredits: '0.00',
      balance: '2450000.00',
    },
    {
      accountId: SALES.id,
      accountName: 'Sales',
      accountType: 'REVENUE',
      totalDebits: '0.00',
      totalCredits: '2450000.00',
      balance: '-2450000.00',
    },
  ],
};

export const ENTRY: JournalEntry = {
  id: '99999999-9999-9999-9999-999999999999',
  description: 'First sale',
  status: 'POSTED',
  createdAt: '2026-10-07T08:00:00Z',
  postedAt: '2026-10-07T09:00:00Z',
  reversalOfId: null,
  lines: [
    { accountId: CASH.id, amount: '2450000.00', entryType: 'DEBIT' },
    { accountId: SALES.id, amount: '2450000.00', entryType: 'CREDIT' },
  ],
};

export const ENTRIES: PagedJournalEntries = {
  content: [ENTRY],
  page: 0,
  size: 5,
  totalElements: 1,
  totalPages: 1,
};
