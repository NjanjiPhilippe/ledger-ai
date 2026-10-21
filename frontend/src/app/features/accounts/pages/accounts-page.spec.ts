import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { CASH, OLD_SUSPENSE, pageOf } from '../../../../testing/accounts';
import { translocoTesting } from '../../../../testing/transloco';
import { PagedAccounts } from '../../../core/api/api-types';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/toast/toast.service';
import { AccountsApi } from '../data-access/accounts.api';
import { AccountsPage } from './accounts-page';

describe('AccountsPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let create: ReturnType<typeof vi.fn>;
  let update: ReturnType<typeof vi.fn>;

  function configure(roles: string[] = ['viewer']) {
    list = vi.fn();
    create = vi.fn();
    update = vi.fn();
    TestBed.configureTestingModule({
      imports: [AccountsPage, translocoTesting()],
      providers: [
        { provide: AccountsApi, useValue: { list, create, update } },
        { provide: AuthService, useValue: { appRoles: signal(roles) } },
      ],
    });
  }

  beforeEach(() => configure());

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
    expect(list).toHaveBeenCalledWith(0, 20, {});
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
    expect(list).toHaveBeenLastCalledWith(1, 20, {});
  });
});

describe('AccountsPage actions', () => {
  const list = vi.fn();
  const create = vi.fn();
  const update = vi.fn();

  function open(roles: string[]) {
    list.mockReset().mockReturnValue(of(pageOf([CASH, OLD_SUSPENSE], { totalElements: 2 })));
    create.mockReset();
    update.mockReset();
    TestBed.configureTestingModule({
      imports: [AccountsPage, translocoTesting()],
      providers: [
        { provide: AccountsApi, useValue: { list, create, update } },
        { provide: AuthService, useValue: { appRoles: signal(roles) } },
      ],
    });
    const fixture = TestBed.createComponent(AccountsPage);
    return fixture.whenStable().then(() => ({
      fixture,
      element: fixture.nativeElement as HTMLElement,
    }));
  }

  const q = (element: HTMLElement, testId: string) =>
    element.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null;

  function type(element: HTMLElement, selector: string, value: string) {
    const input = element.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  it('offers nothing to change to a viewer', async () => {
    const { element } = await open(['viewer']);

    expect(q(element, 'new-account')).toBeNull();
    expect(q(element, 'edit-account')).toBeNull();
  });

  it('lets an accountant create an account, but not edit one', async () => {
    const { element } = await open(['accountant']);

    expect(q(element, 'new-account')).not.toBeNull();
    expect(q(element, 'edit-account')).toBeNull();
  });

  it('creates an account, confirms with a toast, closes the form and reloads the list', async () => {
    const { fixture, element } = await open(['accountant']);
    create.mockReturnValue(of({ ...CASH, id: 'new', name: 'Petty cash' }));

    q(element, 'new-account')?.click();
    await fixture.whenStable();
    type(element, '#account-name', 'Petty cash');
    q(element, 'save')?.click();
    await fixture.whenStable();

    expect(create).toHaveBeenCalledWith({ name: 'Petty cash', type: 'ASSET', currencyCode: 'XAF' });
    expect(TestBed.inject(ToastService).toasts()[0]).toMatchObject({
      level: 'success',
      title: 'Account "Petty cash" created',
    });
    expect(q(element, 'account-form')).toBeNull();
    expect(list).toHaveBeenCalledTimes(2);
  });

  it('keeps the form open, with what was typed, when the server refuses', async () => {
    const { fixture, element } = await open(['accountant']);
    create.mockReturnValue(throwError(() => new Error('409')));

    q(element, 'new-account')?.click();
    await fixture.whenStable();
    type(element, '#account-name', 'Cash');
    q(element, 'save')?.click();
    await fixture.whenStable();

    expect(q(element, 'account-form')).not.toBeNull();
    expect((element.querySelector('#account-name') as HTMLInputElement).value).toBe('Cash');
    expect(TestBed.inject(ToastService).toasts()).toHaveLength(0);
  });

  it('refuses a blank name without calling the server', async () => {
    const { fixture, element } = await open(['accountant']);

    q(element, 'new-account')?.click();
    await fixture.whenStable();
    q(element, 'save')?.click();
    await fixture.whenStable();

    expect(create).not.toHaveBeenCalled();
    expect(q(element, 'name-error')?.textContent).toContain('Enter a name');
  });

  it('refuses a currency that is not three letters', async () => {
    const { fixture, element } = await open(['accountant']);

    q(element, 'new-account')?.click();
    await fixture.whenStable();
    type(element, '#account-name', 'Bank');
    type(element, '#account-currency', 'EU');
    q(element, 'save')?.click();
    await fixture.whenStable();

    expect(create).not.toHaveBeenCalled();
    expect(q(element, 'currency-error')).not.toBeNull();
  });

  it('lets an administrator rename an account and deactivate it', async () => {
    const { fixture, element } = await open(['admin']);
    update.mockReturnValue(of({ ...CASH, name: 'Main cash', active: false }));

    q(element, 'edit-account')?.click();
    await fixture.whenStable();
    type(element, '#account-name', 'Main cash');
    (element.querySelector('input[type="checkbox"]') as HTMLInputElement).click();
    q(element, 'save')?.click();
    await fixture.whenStable();

    expect(update).toHaveBeenCalledWith(CASH.id, { name: 'Main cash', active: false });
    expect(q(element, 'account-form')).toBeNull();
  });

  it('does not offer to change the type or the currency of an existing account', async () => {
    const { fixture, element } = await open(['admin']);

    q(element, 'edit-account')?.click();
    await fixture.whenStable();

    expect(element.querySelector('#account-type')).toBeNull();
    expect(element.querySelector('#account-currency')).toBeNull();
  });

  it('searches after a pause in typing, and filters by type and status, from the first page', async () => {
    const { fixture, element } = await open(['viewer']);
    vi.useFakeTimers();
    try {
      type(element, '[data-testid="search"]', 'cash');
      type(element, '[data-testid="search"]', 'cash ');
      expect(list).toHaveBeenCalledTimes(1);
      vi.advanceTimersByTime(300);
      expect(list).toHaveBeenLastCalledWith(0, 20, { name: 'cash' });
    } finally {
      vi.useRealTimers();
    }

    const typeFilter = q(element, 'type-filter') as HTMLSelectElement;
    typeFilter.value = 'ASSET';
    typeFilter.dispatchEvent(new Event('change'));
    expect(list).toHaveBeenLastCalledWith(0, 20, { name: 'cash', type: 'ASSET' });

    const status = q(element, 'status-filter') as HTMLSelectElement;
    status.value = 'inactive';
    status.dispatchEvent(new Event('change'));
    expect(list).toHaveBeenLastCalledWith(0, 20, { name: 'cash', type: 'ASSET', active: false });

    await fixture.whenStable();
    q(element, 'clear-filters')?.click();
    expect(list).toHaveBeenLastCalledWith(0, 20, {});
  });
});
