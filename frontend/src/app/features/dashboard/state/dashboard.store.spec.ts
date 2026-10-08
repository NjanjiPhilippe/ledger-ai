import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ACCOUNTS, ENTRIES, TRIAL_BALANCE } from '../../../../testing/ledger';
import { DashboardApi } from '../data-access/dashboard.api';
import { DashboardStore } from './dashboard.store';

describe('DashboardStore', () => {
  const api = {
    trialBalance: vi.fn(),
    recentEntries: vi.fn(),
    accounts: vi.fn(),
  };

  function create(): DashboardStore {
    TestBed.configureTestingModule({
      providers: [DashboardStore, { provide: DashboardApi, useValue: api }],
    });
    return TestBed.inject(DashboardStore);
  }

  beforeEach(() => {
    api.trialBalance.mockReset().mockReturnValue(of(TRIAL_BALANCE));
    api.recentEntries.mockReset().mockReturnValue(of(ENTRIES));
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
  });

  it('starts every section as loading', () => {
    const store = create();

    expect(store.trialBalance().status).toBe('loading');
    expect(store.entries().status).toBe('loading');
    expect(store.accounts().status).toBe('loading');
  });

  it('loads the three sections', () => {
    const store = create();
    store.load();

    expect(store.trialBalance()).toEqual({ status: 'loaded', data: TRIAL_BALANCE });
    expect(store.entries().data).toBe(ENTRIES);
    expect(api.recentEntries).toHaveBeenCalledWith(5);
    expect(api.accounts).toHaveBeenCalledWith(100);
  });

  it('counts accounts: all, active, and by type', () => {
    const store = create();
    store.load();

    expect(store.totalAccounts()).toBe(3);
    expect(store.activeAccounts()).toBe(2);
    expect(store.accountsByType().get('ASSET')).toBe(1);
    expect(store.accountsByType().get('EXPENSE')).toBe(1);
    expect(store.accountsByType().get('EQUITY')).toBeUndefined();
  });

  it('maps account ids to names, from the accounts and from the trial balance', () => {
    const store = create();
    api.accounts.mockReturnValue(throwError(() => new Error('down')));
    store.load();

    expect(store.accountNames().get(TRIAL_BALANCE.lines[0].accountId)).toBe('Cash');
  });

  it('keeps a failing section isolated and lets it be retried', () => {
    api.trialBalance.mockReturnValueOnce(throwError(() => new Error('403')));
    const store = create();
    store.load();

    expect(store.trialBalance()).toEqual({ status: 'error', data: null });
    expect(store.entries().status).toBe('loaded');
    expect(store.accounts().status).toBe('loaded');

    store.loadTrialBalance();
    expect(store.trialBalance().status).toBe('loaded');
  });

  it('goes back to loading while a retry is in flight', () => {
    const pending = new Subject<typeof TRIAL_BALANCE>();
    const store = create();
    store.load();
    api.trialBalance.mockReturnValueOnce(pending);

    store.loadTrialBalance();

    expect(store.trialBalance().status).toBe('loading');
    pending.next(TRIAL_BALANCE);
    expect(store.trialBalance().status).toBe('loaded');
  });
});
