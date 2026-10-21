import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { JournalEntry } from '../../../core/api/api-types';
import { ACCOUNTS, DRAFT_ENTRY, ENTRY, REVERSAL_ENTRY } from '../../../../testing/ledger';
import { EntriesApi } from '../data-access/entries.api';
import { EntryDetailStore } from './entry-detail.store';

describe('EntryDetailStore', () => {
  const api = { get: vi.fn(), accounts: vi.fn(), post: vi.fn(), reverse: vi.fn() };
  let store: EntryDetailStore;

  beforeEach(() => {
    api.get.mockReset().mockReturnValue(of(ENTRY));
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
    api.post.mockReset();
    api.reverse.mockReset();
    TestBed.configureTestingModule({
      providers: [EntryDetailStore, { provide: EntriesApi, useValue: api }],
    });
    store = TestBed.inject(EntryDetailStore);
  });

  it('shows the lines with account names, the currency and exact totals', () => {
    store.load(ENTRY.id);

    expect(store.status()).toBe('loaded');
    expect(store.lines().map((l) => [l.accountName, l.side])).toEqual([
      ['Cash', 'DEBIT'],
      ['Sales', 'CREDIT'],
    ]);
    expect(store.currency()).toBe('XAF');
    expect(store.totalDebits()).toBe('2450000.00');
    expect(store.totalCredits()).toBe('2450000.00');
    expect(store.balanced()).toBe(true);
  });

  it('has no name for an account it does not know, and never falls back to an identifier', () => {
    api.accounts.mockReturnValue(throwError(() => new Error('down')));

    store.load(ENTRY.id);

    expect(store.lines().every((l) => l.accountName === null)).toBe(true);
  });

  it('tells a missing entry from a failure', () => {
    api.get.mockReturnValue(throwError(() => ({ status: 404 })));
    store.load('nope');
    expect(store.status()).toBe('notFound');

    api.get.mockReturnValue(throwError(() => ({ status: 500 })));
    store.load('nope');
    expect(store.status()).toBe('error');
  });

  it('loads the original of a reversal', () => {
    api.get.mockImplementation((id: string) => of(id === ENTRY.id ? ENTRY : REVERSAL_ENTRY));

    store.load(REVERSAL_ENTRY.id);

    expect(store.original()?.description).toBe('First sale');
  });

  it('posting replaces the entry on screen with the posted one', () => {
    api.get.mockReturnValue(of(DRAFT_ENTRY));
    store.load(DRAFT_ENTRY.id);
    api.post.mockReturnValue(of({ ...DRAFT_ENTRY, status: 'POSTED' }));

    store.post().subscribe();

    expect(api.post).toHaveBeenCalledWith(DRAFT_ENTRY.id);
    expect(store.entry()?.status).toBe('POSTED');
  });

  it('reversing returns the reversal, which is a new entry', () => {
    store.load(ENTRY.id);
    api.reverse.mockReturnValue(of(REVERSAL_ENTRY));

    let result: unknown;
    store.reverse().subscribe((entry) => (result = entry));

    expect(api.reverse).toHaveBeenCalledWith(ENTRY.id);
    expect(result).toBe(REVERSAL_ENTRY);
  });

  it('ignores a slow answer for an entry the person has already left', () => {
    const slow = new Subject<JournalEntry>();
    api.get.mockImplementation((id: string) => (id === ENTRY.id ? slow : of(DRAFT_ENTRY)));

    store.load(ENTRY.id);
    store.load(DRAFT_ENTRY.id);
    slow.next(ENTRY);

    expect(store.entry()?.id).toBe(DRAFT_ENTRY.id);
    expect(store.status()).toBe('loaded');
  });

  it('shows no entry while the next one loads', () => {
    store.load(ENTRY.id);
    api.get.mockReturnValue(new Subject<JournalEntry>());

    store.load(DRAFT_ENTRY.id);

    expect(store.entry()).toBeNull();
    expect(store.status()).toBe('loading');
  });

  it('still shows amounts, without a currency, when the accounts could not be loaded', () => {
    api.accounts.mockReturnValue(throwError(() => new Error('down')));

    store.load(ENTRY.id);

    expect(store.currency()).toBe('');
  });
});
