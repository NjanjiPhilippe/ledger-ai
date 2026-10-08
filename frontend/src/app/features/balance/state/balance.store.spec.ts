import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FULL_TRIAL_BALANCE } from '../../../../testing/ledger';
import { BalanceApi } from '../data-access/balance.api';
import { BalanceStore } from './balance.store';

describe('BalanceStore', () => {
  const api = { trialBalance: vi.fn() };
  let store: BalanceStore;

  beforeEach(() => {
    api.trialBalance.mockReset().mockReturnValue(of(FULL_TRIAL_BALANCE));
    TestBed.configureTestingModule({
      providers: [BalanceStore, { provide: BalanceApi, useValue: api }],
    });
    store = TestBed.inject(BalanceStore);
  });

  it('groups the accounts by type, in accounting order, with subtotals', () => {
    store.load();

    expect(store.status()).toBe('loaded');
    expect(store.groups().map((g) => g.type)).toEqual(['ASSET', 'EQUITY', 'REVENUE', 'EXPENSE']);
    const assets = store.groups()[0];
    expect(assets.totalDebits).toBe('1950000.00');
    expect(assets.lines.map((l) => l.accountName)).toEqual(['Cash']);
  });

  it('hides the accounts without movement unless asked, and says how many', () => {
    store.load();
    expect(store.shownCount()).toBe(4);
    expect(store.hiddenCount()).toBe(1);
    expect(store.groups().some((g) => g.type === 'LIABILITY')).toBe(false);

    store.hideEmpty.set(false);
    expect(store.shownCount()).toBe(5);
    expect(store.groups().some((g) => g.type === 'LIABILITY')).toBe(true);
  });

  it('counts the accounts with movements', () => {
    store.load();

    expect(store.movementCount()).toBe(4);
  });

  it('searches by name, whatever the case', () => {
    store.load();

    store.search.set('  SAL ');

    expect(store.groups().flatMap((g) => g.lines.map((l) => l.accountName))).toEqual(['Sales']);
  });

  it('computes the accounting equation exactly', () => {
    store.load();

    expect(store.summary()).toEqual({
      assets: '1950000.00',
      liabilitiesAndEquity: '1500000.00',
      netResult: '450000.00',
    });
  });

  it('reports the gap between debits and credits', () => {
    api.trialBalance.mockReturnValue(
      of({ ...FULL_TRIAL_BALANCE, balanced: false, totalCredits: '2449999.50' }),
    );
    store.load();

    expect(store.gap()).toBe('0.50');
  });

  it('goes to the error state, and a retry loads again', () => {
    api.trialBalance.mockReturnValueOnce(throwError(() => new Error('down')));
    store.load();
    expect(store.status()).toBe('error');

    store.load();
    expect(store.status()).toBe('loaded');
  });

  it('has nothing to show before the report arrives', () => {
    expect(store.groups()).toEqual([]);
    expect(store.gap()).toBe('0');
    expect(store.status()).toBe('loading');
  });
});
