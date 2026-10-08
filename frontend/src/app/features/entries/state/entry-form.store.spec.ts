import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { CASH, OLD_SUSPENSE, pageOf } from '../../../../testing/accounts';
import { SALES } from '../../../../testing/ledger';
import { EntriesApi } from '../data-access/entries.api';
import { EntryFormStore } from './entry-form.store';

const EUR_BANK = { ...CASH, id: 'eur-1', name: 'Euro bank', currencyCode: 'EUR' };

describe('EntryFormStore', () => {
  const api = { activeAccounts: vi.fn(), record: vi.fn(), post: vi.fn() };

  function create(accounts = [CASH, { ...SALES }, EUR_BANK]) {
    api.activeAccounts.mockReset().mockReturnValue(of(pageOf(accounts as never)));
    api.record.mockReset();
    api.post.mockReset();
    TestBed.configureTestingModule({
      providers: [EntryFormStore, { provide: EntriesApi, useValue: api }],
    });
    const store = TestBed.inject(EntryFormStore);
    store.load();
    return store;
  }

  /** Fills the two starting lines with a debit on Cash and a credit on Sales. */
  function fill(store: EntryFormStore, debit = '1500', credit = '1500') {
    const [first, second] = store.lines();
    store.description.set('Sale');
    store.updateLine(first.id, { accountId: CASH.id, amount: debit });
    store.updateLine(second.id, { accountId: SALES.id, amount: credit });
  }

  it('starts with one debit line, one credit line, and the most used currency', () => {
    const store = create();

    expect(store.accountsStatus()).toBe('loaded');
    expect(store.lines().map((l) => l.side)).toEqual(['DEBIT', 'CREDIT']);
    expect(store.currency()).toBe('XAF');
    expect(store.currencies()).toEqual(['EUR', 'XAF']);
    expect(store.valid()).toBe(false);
  });

  it('only offers the accounts of the chosen currency, and drops a line account of another one', () => {
    const store = create();
    fill(store);

    store.setCurrency('EUR');

    expect(store.eligibleAccounts().map((a) => a.name)).toEqual(['Euro bank']);
    expect(store.lines().every((l) => l.accountId === '')).toBe(true);
  });

  it('adds up exactly, and is balanced only when debits equal credits', () => {
    const store = create();
    fill(store, '0.1', '0.3');
    store.addLine('DEBIT');
    store.updateLine(store.lines()[2].id, { amount: '0.2' });

    expect(store.totalDebits()).toBe('0.3');
    expect(store.totalCredits()).toBe('0.3');
    expect(store.balanced()).toBe(true);
  });

  it('accepts amounts typed with spaces and a decimal comma', () => {
    const store = create();
    fill(store, '1 500,50', '1500.5');

    expect(store.totalDebits()).toBe('1500.50');
    expect(store.balanced()).toBe(true);
  });

  it('reports the difference when debits and credits differ', () => {
    const store = create();
    fill(store, '1000', '400');

    expect(store.balanced()).toBe(false);
    expect(store.gapAmount()).toBe('600');
  });

  it('flags a missing account and an amount that is empty, negative, zero or not a number', () => {
    const store = create();
    const [first, second] = store.lines();
    store.updateLine(second.id, { accountId: SALES.id });

    expect(store.problems().get(first.id)).toEqual({ account: true, amount: true });
    for (const bad of ['', '0', '-5', 'abc', '1.2.3']) {
      store.updateLine(second.id, { amount: bad });
      expect(store.problems().get(second.id)?.amount).toBe(true);
    }
    store.updateLine(second.id, { amount: '12.5' });
    expect(store.problems().get(second.id)).toEqual({ account: false, amount: false });
  });

  it('needs at least a debit and a credit, a description, and two lines', () => {
    const store = create();
    fill(store);
    expect(store.valid()).toBe(true);

    store.updateLine(store.lines()[1].id, { side: 'DEBIT' });
    expect(store.hasBothSides()).toBe(false);
    expect(store.valid()).toBe(false);

    store.updateLine(store.lines()[1].id, { side: 'CREDIT' });
    store.description.set('   ');
    expect(store.valid()).toBe(false);
  });

  it('proposes the missing amount on a new line of the side that is short', () => {
    const store = create();
    fill(store, '1000', '400');

    store.addLine('CREDIT');
    expect(store.lines()[2].amount).toBe('600');

    store.addLine('DEBIT');
    expect(store.lines()[3].amount).toBe('');
  });

  it('keeps at least two lines', () => {
    const store = create();
    const [first] = store.lines();

    store.removeLine(first.id);
    expect(store.lines()).toHaveLength(2);

    store.addLine('DEBIT');
    store.removeLine(store.lines()[2].id);
    expect(store.lines()).toHaveLength(2);
  });

  it('shows no error until a save was attempted', () => {
    const store = create();
    expect(store.submitted()).toBe(false);

    expect(store.validate()).toBe(false);
    expect(store.submitted()).toBe(true);
  });

  it('sends the entry as exact strings, in the chosen currency', () => {
    const store = create();
    fill(store, '1 500,50', '1500.50');
    api.record.mockReturnValue(of({ id: 'e1' }));

    store.submit(false).subscribe();

    expect(api.record).toHaveBeenCalledWith({
      description: 'Sale',
      currencyCode: 'XAF',
      lines: [
        { accountId: CASH.id, entryType: 'DEBIT', amount: '1500.50' },
        { accountId: SALES.id, entryType: 'CREDIT', amount: '1500.50' },
      ],
    });
    expect(api.post).not.toHaveBeenCalled();
    expect(store.savedDraft()).toEqual({ id: 'e1' });
  });

  it('posts the entry right after saving it when asked', () => {
    const store = create();
    fill(store);
    api.record.mockReturnValue(of({ id: 'e1' }));
    api.post.mockReturnValue(of({ id: 'e1', status: 'POSTED' }));

    let result: unknown;
    store.submit(true).subscribe((entry) => (result = entry));

    expect(api.post).toHaveBeenCalledWith('e1');
    expect(result).toEqual({ id: 'e1', status: 'POSTED' });
  });

  it('remembers the draft when only the posting fails', () => {
    const store = create();
    fill(store);
    api.record.mockReturnValue(of({ id: 'e1' }));
    api.post.mockReturnValue(throwError(() => new Error('403')));

    store.submit(true).subscribe({ error: () => undefined });

    expect(store.savedDraft()).toEqual({ id: 'e1' });
  });

  it('goes to the error state when the accounts cannot be loaded, and can retry', () => {
    const store = create();
    api.activeAccounts.mockReturnValueOnce(throwError(() => new Error('down')));
    store.load();
    expect(store.accountsStatus()).toBe('error');

    store.load();
    expect(store.accountsStatus()).toBe('loaded');
  });

  it('works with no account at all', () => {
    const store = create([]);

    expect(store.currency()).toBe('');
    expect(store.currencies()).toEqual([]);
    expect(OLD_SUSPENSE.active).toBe(false);
  });
});
