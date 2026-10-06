import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CASH, OLD_SUSPENSE, pageOf } from '../../../../testing/accounts';
import { translocoTesting } from '../../../../testing/transloco';
import { PagedAccounts } from '../../../core/api/api-types';
import { AccountsApi } from '../data-access/accounts.api';
import { AccountsPage } from './accounts-page';

describe('AccountsPage', () => {
  let list: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    list = vi.fn();
    TestBed.configureTestingModule({
      imports: [AccountsPage, translocoTesting()],
      providers: [{ provide: AccountsApi, useValue: { list } }],
    });
  });

  async function open() {
    const fixture = TestBed.createComponent(AccountsPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const text = (element: HTMLElement, testId: string) =>
    element.querySelector(`[data-testid="${testId}"]`)?.textContent?.replace(/\s+/g, ' ').trim();

  it('shows a loading message while the accounts are on their way', async () => {
    list.mockReturnValue(new Subject<PagedAccounts>());

    const { element } = await open();

    expect(text(element, 'loading')).toBe('Loading…');
    expect(list).toHaveBeenCalledWith(0, 20);
  });

  it('lists the accounts with their count and the page position', async () => {
    list.mockReturnValue(of(pageOf([CASH, OLD_SUSPENSE], { totalElements: 2, totalPages: 1 })));

    const { element } = await open();

    expect(element.querySelectorAll('[data-testid="account-row"]')).toHaveLength(2);
    expect(text(element, 'total')).toBe('2 accounts');
    expect(text(element, 'page-of')).toBe('Page 1 of 1');
  });

  it('says so when there is no account yet', async () => {
    list.mockReturnValue(of(pageOf([])));

    const { element } = await open();

    expect(text(element, 'empty')).toBe('There are no accounts yet.');
    expect(element.querySelector('table')).toBeNull();
  });

  it('offers a retry when loading fails, and the retry loads again', async () => {
    list.mockReturnValueOnce(throwError(() => new Error('boom')));
    const { fixture, element } = await open();
    expect(text(element, 'error')).toContain('The accounts could not be loaded.');

    list.mockReturnValueOnce(of(pageOf([CASH])));
    (element.querySelector('[data-testid="error"] button') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="error"]')).toBeNull();
    expect(element.querySelectorAll('[data-testid="account-row"]')).toHaveLength(1);
  });

  it('disables the pagination buttons at the ends and moves forward when allowed', async () => {
    list.mockReturnValue(of(pageOf([CASH], { page: 0, totalPages: 3, totalElements: 41 })));
    const { fixture, element } = await open();

    const [previous, next] = Array.from(
      element.querySelectorAll('nav button'),
    ) as HTMLButtonElement[];
    expect(previous.disabled).toBe(true);
    expect(next.disabled).toBe(false);

    next.click();
    await fixture.whenStable();
    expect(list).toHaveBeenLastCalledWith(1, 20);
  });
});
