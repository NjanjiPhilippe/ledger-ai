import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CASH, pageOf } from '../../../../testing/accounts';
import { SALES } from '../../../../testing/ledger';
import { translocoTesting } from '../../../../testing/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { ToastService } from '../../../core/toast/toast.service';
import { EntriesApi } from '../data-access/entries.api';
import { NewEntryPage } from './new-entry-page';

describe('NewEntryPage', () => {
  const api = { activeAccounts: vi.fn(), record: vi.fn(), post: vi.fn() };
  const navigate = vi.fn();

  async function open(accounts: unknown[] = [CASH, SALES]) {
    localStorage.clear();
    api.activeAccounts.mockReset().mockReturnValue(of(pageOf(accounts as never)));
    api.record.mockReset().mockReturnValue(of({ id: 'e1', status: 'DRAFT' }));
    api.post.mockReset().mockReturnValue(of({ id: 'e1', status: 'POSTED' }));
    navigate.mockReset();
    TestBed.configureTestingModule({
      imports: [NewEntryPage, translocoTesting()],
      providers: [provideRouter([]), { provide: EntriesApi, useValue: api }],
    });
    TestBed.inject(LanguageService).initialize();
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockImplementation(navigate);
    const fixture = TestBed.createComponent(NewEntryPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const q = (element: HTMLElement, testId: string) =>
    element.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null;
  const all = (element: HTMLElement, testId: string) =>
    Array.from(element.querySelectorAll(`[data-testid="${testId}"]`)) as HTMLElement[];
  const text = (element: HTMLElement, testId: string) =>
    q(element, testId)?.textContent?.replace(/\s+/g, ' ').trim();

  function type(input: HTMLInputElement, value: string) {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function choose(select: HTMLSelectElement, value: string) {
    select.value = value;
    select.dispatchEvent(new Event('change'));
  }

  /** Fills the form with a balanced sale of 1 500. */
  async function fillBalanced(
    fixture: { whenStable: () => Promise<unknown> },
    element: HTMLElement,
  ) {
    type(element.querySelector('#entry-description') as HTMLInputElement, 'Sale');
    const accounts = all(element, 'line-account') as HTMLSelectElement[];
    choose(accounts[0], CASH.id);
    choose(accounts[1], SALES.id);
    const amounts = all(element, 'line-amount') as HTMLInputElement[];
    type(amounts[0], '1500');
    type(amounts[1], '1 500');
    await fixture.whenStable();
  }

  it('starts with a debit line and a credit line, and lists accounts by name', async () => {
    const { element } = await open();

    expect(all(element, 'entry-line')).toHaveLength(2);
    const options = Array.from(element.querySelectorAll('[data-testid="line-account"] option'))
      .slice(0, 3)
      .map((o) => o.textContent?.trim());
    expect(options).toEqual(['Choose an account', 'Cash', 'Sales']);
    expect(element.textContent).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-/);
  });

  it('shows the totals and the difference as the person types', async () => {
    const { fixture, element } = await open();
    const amounts = all(element, 'line-amount') as HTMLInputElement[];

    type(amounts[0], '1500');
    type(amounts[1], '400');
    await fixture.whenStable();

    expect(text(element, 'total-debits')).toMatch(/1,500/);
    expect(text(element, 'total-credits')).toMatch(/400/);
    expect(text(element, 'balance-status')).toMatch(/1,100/);

    type(amounts[1], '1500');
    await fixture.whenStable();
    expect(text(element, 'balance-status')).toContain('Balanced');
  });

  it('shows what is wrong, and sends nothing, when saving an incomplete entry', async () => {
    const { fixture, element } = await open();

    q(element, 'save-draft')?.click();
    await fixture.whenStable();

    expect(api.record).not.toHaveBeenCalled();
    expect(q(element, 'description-error')).not.toBeNull();
    expect(all(element, 'line-account-error')).toHaveLength(2);
    expect(all(element, 'line-amount-error')).toHaveLength(2);
  });

  it('refuses an entry that is not balanced', async () => {
    const { fixture, element } = await open();
    await fillBalanced(fixture, element);
    type((all(element, 'line-amount') as HTMLInputElement[])[1], '1400');
    await fixture.whenStable();

    q(element, 'save-post')?.click();
    await fixture.whenStable();

    expect(api.record).not.toHaveBeenCalled();
    expect(text(element, 'balance-error')).toContain('must be equal');
  });

  it('saves a draft, says so, and goes back to the dashboard', async () => {
    const { fixture, element } = await open();
    await fillBalanced(fixture, element);

    q(element, 'save-draft')?.click();
    await fixture.whenStable();

    expect(api.record).toHaveBeenCalledWith({
      description: 'Sale',
      currencyCode: 'XAF',
      lines: [
        { accountId: CASH.id, entryType: 'DEBIT', amount: '1500' },
        { accountId: SALES.id, entryType: 'CREDIT', amount: '1500' },
      ],
    });
    expect(api.post).not.toHaveBeenCalled();
    expect(TestBed.inject(ToastService).toasts()[0].title).toBe('Draft "Sale" saved');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('saves and posts in one go', async () => {
    const { fixture, element } = await open();
    await fillBalanced(fixture, element);

    q(element, 'save-post')?.click();
    await fixture.whenStable();

    expect(api.post).toHaveBeenCalledWith('e1');
    expect(TestBed.inject(ToastService).toasts()[0].title).toBe('Entry "Sale" posted');
  });

  it('stays on the form, with everything typed, when the server refuses the entry', async () => {
    const { fixture, element } = await open();
    await fillBalanced(fixture, element);
    api.record.mockReturnValue(throwError(() => new Error('400')));

    q(element, 'save-post')?.click();
    await fixture.whenStable();

    expect(navigate).not.toHaveBeenCalled();
    expect((element.querySelector('#entry-description') as HTMLInputElement).value).toBe('Sale');
    expect((q(element, 'save-post') as HTMLButtonElement).disabled).toBe(false);
  });

  it('tells when only the posting failed, because the draft exists', async () => {
    const { fixture, element } = await open();
    await fillBalanced(fixture, element);
    api.post.mockReturnValue(throwError(() => new Error('403')));

    q(element, 'save-post')?.click();
    await fixture.whenStable();

    expect(TestBed.inject(ToastService).toasts()[0].title).toContain('saved as a draft');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('adds and removes lines, and keeps two at least', async () => {
    const { fixture, element } = await open();

    q(element, 'add-credit')?.click();
    await fixture.whenStable();
    expect(all(element, 'entry-line')).toHaveLength(3);

    all(element, 'remove-line')[2].click();
    await fixture.whenStable();
    expect(all(element, 'entry-line')).toHaveLength(2);
    expect(all(element, 'remove-line').every((b) => (b as HTMLButtonElement).disabled)).toBe(true);
  });

  it('asks to create accounts first when there are fewer than two', async () => {
    const { element } = await open([CASH]);

    expect(q(element, 'no-accounts')).not.toBeNull();
    expect(q(element, 'save-post')).toBeNull();
  });

  it('offers a retry when the accounts cannot be loaded', async () => {
    const { fixture, element } = await open();
    api.activeAccounts.mockReturnValueOnce(throwError(() => new Error('down')));
    (fixture.componentInstance as unknown as { store: { load(): void } }).store.load();
    await fixture.whenStable();
    expect(q(element, 'error')).not.toBeNull();

    (q(element, 'error')?.querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(q(element, 'error')).toBeNull();
  });
});
