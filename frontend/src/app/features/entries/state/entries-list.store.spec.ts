import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { PagedJournalEntries } from '../../../core/api/api-types';
import { ACCOUNTS, DRAFT_ENTRY, ENTRIES, ENTRY } from '../../../../testing/ledger';
import { EntriesApi } from '../data-access/entries.api';
import { EntriesListStore } from './entries-list.store';

describe('EntriesListStore', () => {
  const api = { list: vi.fn(), accounts: vi.fn() };
  let store: EntriesListStore;

  const page = (
    content = [ENTRY],
    extra: Partial<PagedJournalEntries> = {},
  ): PagedJournalEntries => ({
    ...ENTRIES,
    content,
    totalElements: content.length,
    ...extra,
  });

  beforeEach(() => {
    api.list.mockReset();
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
    TestBed.configureTestingModule({
      providers: [EntriesListStore, { provide: EntriesApi, useValue: api }],
    });
    store = TestBed.inject(EntriesListStore);
  });

  it('loads a page of 20 and exposes it', () => {
    api.list.mockReturnValue(of(page([ENTRY, DRAFT_ENTRY])));

    store.load(0);

    expect(api.list).toHaveBeenCalledWith(0, 20, {});
    expect(store.status()).toBe('loaded');
    expect(store.entries()).toHaveLength(2);
    expect(store.totalElements()).toBe(2);
  });

  it('maps account ids to names', () => {
    store.loadAccountNames();

    expect(store.accountNames().get(ACCOUNTS.content[0].id)).toBe('Cash');
  });

  it('still lists the entries when the account names cannot be loaded', () => {
    api.accounts.mockReturnValue(throwError(() => new Error('down')));
    api.list.mockReturnValue(of(page()));

    store.loadAccountNames();
    store.load(0);

    expect(store.status()).toBe('loaded');
    expect(store.accountNames().size).toBe(0);
  });

  it('filters from the first page, drops empty filters, and can clear them', () => {
    api.list.mockReturnValue(of(page()));
    store.load(3);

    store.filter({ status: 'POSTED', createdFrom: '2026-10-01T00:00:00Z' });
    expect(api.list).toHaveBeenLastCalledWith(0, 20, {
      status: 'POSTED',
      createdFrom: '2026-10-01T00:00:00Z',
    });
    expect(store.hasFilters()).toBe(true);

    store.filter({ status: undefined });
    expect(api.list).toHaveBeenLastCalledWith(0, 20, { createdFrom: '2026-10-01T00:00:00Z' });

    store.clearFilters();
    expect(api.list).toHaveBeenLastCalledWith(0, 20, {});
    expect(store.hasFilters()).toBe(false);
  });

  it('moves between pages and stops at the ends', () => {
    api.list.mockReturnValue(of(page([ENTRY], { page: 0, totalPages: 2 })));
    store.load(0);
    expect(store.hasPrevious()).toBe(false);
    expect(store.hasNext()).toBe(true);

    api.list.mockReturnValue(of(page([ENTRY], { page: 1, totalPages: 2 })));
    store.next();
    expect(api.list).toHaveBeenLastCalledWith(1, 20, {});
    expect(store.hasNext()).toBe(false);

    const calls = api.list.mock.calls.length;
    store.next();
    expect(api.list.mock.calls.length).toBe(calls);
  });

  it('ignores a slow answer for a page the user has already left', () => {
    const slow = new Subject<PagedJournalEntries>();
    api.list.mockReturnValueOnce(slow).mockReturnValueOnce(of(page([DRAFT_ENTRY], { page: 1 })));

    store.load(0);
    store.load(1);
    slow.next(page([ENTRY], { page: 0 }));

    expect(store.entries()).toEqual([DRAFT_ENTRY]);
  });

  it('goes to the error state and retries the same page', () => {
    api.list.mockReturnValueOnce(throwError(() => new Error('boom')));
    store.load(2);
    expect(store.status()).toBe('error');

    api.list.mockReturnValue(of(page()));
    store.load();
    expect(api.list).toHaveBeenLastCalledWith(2, 20, {});
    expect(store.status()).toBe('loaded');
  });
});
