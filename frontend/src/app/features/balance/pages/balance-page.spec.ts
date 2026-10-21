import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FULL_TRIAL_BALANCE } from '../../../../testing/ledger';
import { translocoTesting } from '../../../../testing/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { BalanceApi } from '../data-access/balance.api';
import { BalancePage } from './balance-page';

describe('BalancePage', () => {
  const api = { trialBalance: vi.fn() };

  async function open(report = FULL_TRIAL_BALANCE) {
    localStorage.clear();
    api.trialBalance.mockReset().mockReturnValue(of(report));
    TestBed.configureTestingModule({
      imports: [BalancePage, translocoTesting()],
      providers: [{ provide: BalanceApi, useValue: api }],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(BalancePage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const q = (element: HTMLElement, id: string) =>
    element.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const all = (element: HTMLElement, id: string) =>
    Array.from(element.querySelectorAll(`[data-testid="${id}"]`)) as HTMLElement[];
  const text = (element: HTMLElement, id: string) =>
    q(element, id)?.textContent?.replace(/\s+/g, ' ').trim();

  it('shows the balance grouped by type, with subtotals and a closing total', async () => {
    const { element } = await open();

    expect(all(element, 'balance-group')).toHaveLength(4);
    expect(
      all(element, 'balance-row').map((r) => r.querySelector('td')?.textContent?.trim()),
    ).toEqual(['Cash', 'Share capital', 'Sales', 'Rent']);
    expect(all(element, 'group-total')).toHaveLength(4);
    expect(text(element, 'grand-debits')).toMatch(/2,450,000/);
    expect(text(element, 'grand-credits')).toMatch(/2,450,000/);
  });

  it('says the ledger is balanced, with no gap', async () => {
    const { element } = await open();

    expect(text(element, 'balance-status')).toContain('The ledger is balanced');
    expect(text(element, 'balance-status')).toContain('4 accounts with movements out of 5');
    expect(text(element, 'gap')).toMatch(/^[^1-9]*0[^1-9]*$/);
  });

  it('says so, with the gap, when the ledger is out of balance', async () => {
    const { element } = await open({
      ...FULL_TRIAL_BALANCE,
      balanced: false,
      totalCredits: '2449000.00',
    });

    expect(text(element, 'balance-status')).toContain('out of balance');
    expect(text(element, 'gap')).toMatch(/1,000/);
  });

  it('shows the accounts without movement when asked, and tells how many are hidden', async () => {
    const { fixture, element } = await open();
    expect(text(element, 'hidden-count')).toBe('1 hidden');

    const toggle = q(element, 'hide-empty') as HTMLInputElement;
    toggle.checked = false;
    toggle.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(all(element, 'balance-row')).toHaveLength(5);
    expect(q(element, 'hidden-count')).toBeNull();
  });

  it('searches by name and says when nothing matches', async () => {
    const { fixture, element } = await open();
    const search = q(element, 'search') as HTMLInputElement;

    search.value = 'zzz';
    search.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(q(element, 'empty')).not.toBeNull();
    expect(q(element, 'balance-table')).toBeNull();
  });

  it('shows the accounting equation', async () => {
    const { element } = await open();

    expect(text(element, 'summary-assets')).toMatch(/1,950,000/);
    expect(text(element, 'summary-liabilitiesAndEquity')).toMatch(/1,500,000/);
    expect(text(element, 'summary-netResult')).toMatch(/450,000/);
  });

  it('offers a retry when the balance cannot be loaded', async () => {
    const { fixture, element } = await open();
    expect(q(element, 'error')).toBeNull();
    api.trialBalance.mockReturnValueOnce(throwError(() => new Error('down')));
    (fixture.componentInstance as unknown as { store: { load(): void } }).store.load();
    await fixture.whenStable();
    expect(q(element, 'error')).not.toBeNull();
    expect((q(element, 'export') as HTMLButtonElement).disabled).toBe(false);

    (q(element, 'error')?.querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(q(element, 'error')).toBeNull();
  });

  it('downloads the whole report as a CSV file named after the date', async () => {
    const { element } = await open();
    const create = vi.fn().mockReturnValue('blob:report');
    const revoke = vi.fn();
    vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: revoke });
    const click = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(() => undefined);
    try {
      q(element, 'export')?.click();

      const blob = create.mock.calls[0][0] as Blob;
      expect(blob.type).toContain('text/csv');
      expect(await blob.text()).toContain('Old suspense');
      expect(click).toHaveBeenCalledTimes(1);
      expect(revoke).toHaveBeenCalledWith('blob:report');
    } finally {
      vi.unstubAllGlobals();
      click.mockRestore();
    }
  });
});
