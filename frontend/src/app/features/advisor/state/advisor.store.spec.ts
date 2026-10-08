import { TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { Advice } from '../../../core/api/api-types';
import { ADVICE } from '../../../../testing/ledger';
import { AdvisorApi } from '../data-access/advisor.api';
import { AdvisorStore } from './advisor.store';

describe('AdvisorStore', () => {
  const api = { analyzeLedger: vi.fn() };
  let store: AdvisorStore;

  beforeEach(() => {
    api.analyzeLedger.mockReset().mockReturnValue(of(ADVICE));
    TestBed.configureTestingModule({
      providers: [AdvisorStore, { provide: AdvisorApi, useValue: api }],
    });
    store = TestBed.inject(AdvisorStore);
  });

  it('does nothing until an analysis is asked for', () => {
    expect(store.status()).toBe('idle');
    expect(api.analyzeLedger).not.toHaveBeenCalled();
  });

  it('puts the most serious recommendations first, keeping the advisor order within a severity', () => {
    store.analyze();

    expect(store.status()).toBe('done');
    expect(store.items().map((i) => i.title)).toEqual([
      'Expenses exceed revenue',
      'Low cash cover',
      'Unusual rent',
      'Reinvest idle cash',
    ]);
  });

  it('counts by severity and by category, only for the categories the advisor used', () => {
    store.analyze();

    expect(store.severityCounts()).toEqual({ CRITICAL: 1, WARNING: 2, INFO: 1 });
    expect(store.categories()).toEqual([
      { category: 'LIQUIDITY', count: 1 },
      { category: 'RISK', count: 2 },
      { category: 'GROWTH', count: 1 },
    ]);
  });

  it('filters by category and goes back to everything', () => {
    store.analyze();

    store.category.set('RISK');
    expect(store.visibleItems().map((i) => i.title)).toEqual([
      'Expenses exceed revenue',
      'Unusual rent',
    ]);

    store.category.set('ALL');
    expect(store.visibleItems()).toHaveLength(4);
  });

  it('treats an unknown severity as information and an unknown category as general', () => {
    api.analyzeLedger.mockReturnValue(
      of({
        ...ADVICE,
        recommendations: [{ category: 'ASTROLOGY', severity: 'DOOM', title: 't', detail: 'd' }],
      }),
    );

    store.analyze();

    expect(store.items()[0]).toMatchObject({ severity: 'INFO', category: 'GENERAL' });
  });

  it('is analyzing while the call is in flight, and ignores a second click', () => {
    const response = new Subject<Advice>();
    api.analyzeLedger.mockReturnValue(response);

    store.analyze();
    store.analyze();

    expect(store.status()).toBe('analyzing');
    expect(api.analyzeLedger).toHaveBeenCalledTimes(1);
    response.next(ADVICE);
    expect(store.status()).toBe('done');
  });

  it('goes to the error state when the first analysis fails', () => {
    api.analyzeLedger.mockReturnValue(throwError(() => new Error('502')));

    store.analyze();

    expect(store.status()).toBe('error');
    expect(store.advice()).toBeNull();
  });

  it('keeps the last good analysis when a new one fails', () => {
    store.analyze();
    api.analyzeLedger.mockReturnValue(throwError(() => new Error('502')));

    store.analyze();

    expect(store.status()).toBe('done');
    expect(store.items()).toHaveLength(4);
  });

  it('starts from all categories after a new analysis', () => {
    store.analyze();
    store.category.set('RISK');

    store.analyze();

    expect(store.category()).toBe('ALL');
  });
});
