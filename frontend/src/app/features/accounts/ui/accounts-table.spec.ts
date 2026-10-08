import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { CASH, OLD_SUSPENSE } from '../../../../testing/accounts';
import { translocoTesting } from '../../../../testing/transloco';
import { AccountsTable } from './accounts-table';

describe('AccountsTable', () => {
  async function render() {
    TestBed.configureTestingModule({ imports: [AccountsTable, translocoTesting()] });
    const fixture = TestBed.createComponent(AccountsTable);
    fixture.componentRef.setInput('accounts', [CASH, OLD_SUSPENSE]);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const cells = (row: Element) =>
    Array.from(row.querySelectorAll('td')).map((c) => c.textContent?.trim());

  it('shows one row per account, with the type and the status translated', async () => {
    const { element } = await render();

    const rows = Array.from(element.querySelectorAll('[data-testid="account-row"]'));
    expect(rows.map(cells)).toEqual([
      ['Cash', 'Asset', 'XAF', 'Active'],
      ['Old suspense', 'Liability', 'EUR', 'Inactive'],
    ]);
  });

  it('follows the language', async () => {
    const { fixture, element } = await render();

    TestBed.inject(TranslocoService).setActiveLang('fr');
    await fixture.whenStable();

    const rows = Array.from(element.querySelectorAll('[data-testid="account-row"]'));
    expect(rows.map(cells)).toEqual([
      ['Cash', 'Actif', 'XAF', 'Actif'],
      ['Old suspense', 'Passif', 'EUR', 'Inactif'],
    ]);
    expect(element.querySelector('th')?.textContent?.trim()).toBe('Nom');
  });
});
