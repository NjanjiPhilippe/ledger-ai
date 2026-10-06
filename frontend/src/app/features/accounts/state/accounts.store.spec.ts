import { TestBed } from '@angular/core/testing';
import { Subject, throwError } from 'rxjs';
import { CASH, OLD_SUSPENSE, pageOf } from '../../../../testing/accounts';
import { PagedAccounts } from '../../../core/api/api-types';
import { AccountsApi } from '../data-access/accounts.api';
import { AccountsStore } from './accounts.store';

describe('AccountsStore', () => {
  let list: ReturnType<typeof vi.fn>;
  let store: AccountsStore;

  beforeEach(() => {
    list = vi.fn();
    TestBed.configureTestingModule({
      providers: [AccountsStore, { provide: AccountsApi, useValue: { list } }],
    });
    store = TestBed.inject(AccountsStore);
  });

  /** An API call whose response the test delivers when it chooses to. */
  function pending() {
    const response = new Subject<PagedAccounts>();
    list.mockReturnValueOnce(response);
    return response;
  }

  it('starts idle, with nothing to show', () => {
    expect(store.status()).toBe('idle');
    expect(store.accounts()).toEqual([]);
    expect(store.hasNext()).toBe(false);
    expect(store.hasPrevious()).toBe(false);
  });

  it('is loading until the page arrives, then exposes it', () => {
    const response = pending();

    store.load(0);
    expect(store.status()).toBe('loading');

    response.next(pageOf([CASH, OLD_SUSPENSE], { totalElements: 2 }));
    expect(store.status()).toBe('loaded');
    expect(store.accounts()).toEqual([CASH, OLD_SUSPENSE]);
    expect(store.totalElements()).toBe(2);
    expect(list).toHaveBeenCalledWith(0, 20);
  });

  it('moves between pages and stops at both ends', () => {
    const first = pending();
    store.load(0);
    first.next(pageOf([CASH], { page: 0, totalPages: 2, totalElements: 21 }));
    expect(store.hasNext()).toBe(true);
    expect(store.hasPrevious()).toBe(false);

    const second = pending();
    store.next();
    expect(list).toHaveBeenLastCalledWith(1, 20);
    second.next(pageOf([OLD_SUSPENSE], { page: 1, totalPages: 2, totalElements: 21 }));
    expect(store.hasNext()).toBe(false);
    expect(store.hasPrevious()).toBe(true);

    const callsBefore = list.mock.calls.length;
    store.next(); // already on the last page
    expect(list.mock.calls.length).toBe(callsBefore);

    const back = pending();
    store.previous();
    expect(list).toHaveBeenLastCalledWith(0, 20);
    back.next(pageOf([CASH], { page: 0, totalPages: 2, totalElements: 21 }));
    store.previous(); // already on the first page
    expect(list).toHaveBeenCalledTimes(3);
  });

  it('ignores a slow response for a page the user has already left', () => {
    list.mockReset();
    const slow = pending();
    const fast = pending();

    store.load(0);
    store.load(1);
    fast.next(pageOf([OLD_SUSPENSE], { page: 1, totalPages: 2 }));
    slow.next(pageOf([CASH], { page: 0, totalPages: 2 }));

    expect(store.accounts()).toEqual([OLD_SUSPENSE]);
    expect(store.pageIndex()).toBe(1);
  });

  it('goes to the error state when the call fails, and a retry reloads the same page', () => {
    list.mockReturnValueOnce(throwError(() => new Error('boom')));
    store.load(2);
    expect(store.status()).toBe('error');

    const response = pending();
    store.load(); // retry, without naming the page
    expect(list).toHaveBeenLastCalledWith(2, 20);
    response.next(pageOf([CASH]));
    expect(store.status()).toBe('loaded');
  });
});
