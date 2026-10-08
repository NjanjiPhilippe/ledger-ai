import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ACCOUNTS, DRAFT_ENTRY, ENTRIES, ENTRY } from '../../../../testing/ledger';
import { translocoTesting } from '../../../../testing/transloco';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { EntriesApi } from '../data-access/entries.api';
import { EntriesPage } from './entries-page';

describe('EntriesPage', () => {
  const api = { list: vi.fn(), accounts: vi.fn() };

  async function open(roles: string[] = ['viewer']) {
    localStorage.clear();
    api.list
      .mockReset()
      .mockReturnValue(of({ ...ENTRIES, content: [ENTRY, DRAFT_ENTRY], totalElements: 2 }));
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
    TestBed.configureTestingModule({
      imports: [EntriesPage, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: EntriesApi, useValue: api },
        { provide: AuthService, useValue: { appRoles: signal(roles) } },
      ],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(EntriesPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const q = (element: HTMLElement, id: string) =>
    element.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const text = (element: HTMLElement, id: string) =>
    q(element, id)?.textContent?.replace(/\s+/g, ' ').trim();

  it('lists the entries with the names of their accounts, their status and a link to each', async () => {
    const { element } = await open();

    const rows = Array.from(element.querySelectorAll('[data-testid="entry-row"]'));
    expect(rows).toHaveLength(2);
    expect(rows[0].querySelector('[data-testid="entry-description"]')?.textContent).toBe(
      'First sale',
    );
    expect(rows[0].querySelector('[data-testid="entry-debited"]')?.textContent).toBe('Cash');
    expect(rows[0].querySelector('[data-testid="entry-credited"]')?.textContent).toBe('Sales');
    expect(rows[0].querySelector('[data-testid="entry-status"]')?.textContent?.trim()).toBe(
      'Posted',
    );
    expect(rows[1].querySelector('[data-testid="entry-status"]')?.textContent?.trim()).toBe(
      'Draft',
    );
    expect(rows[0].querySelector('a')?.getAttribute('href')).toBe(`/entries/${ENTRY.id}`);
    expect(text(element, 'total')).toBe('2 entries');
    expect(element.textContent).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-/);
  });

  it('offers "New entry" to an accountant only', async () => {
    expect(q((await open(['viewer'])).element, 'new-entry')).toBeNull();
    TestBed.resetTestingModule();
    expect(q((await open(['accountant'])).element, 'new-entry')).not.toBeNull();
  });

  it('filters by status and by dates, then clears the filters', async () => {
    const { fixture, element } = await open();

    const status = q(element, 'status-filter') as HTMLSelectElement;
    status.value = 'POSTED';
    status.dispatchEvent(new Event('change'));
    expect(api.list).toHaveBeenLastCalledWith(0, 20, { status: 'POSTED' });

    const from = q(element, 'from-filter') as HTMLInputElement;
    from.value = '2026-10-01';
    from.dispatchEvent(new Event('change'));
    const to = q(element, 'to-filter') as HTMLInputElement;
    to.value = '2026-10-31';
    to.dispatchEvent(new Event('change'));
    expect(api.list).toHaveBeenLastCalledWith(0, 20, {
      status: 'POSTED',
      createdFrom: '2026-10-01T00:00:00Z',
      createdTo: '2026-10-31T23:59:59Z',
    });

    await fixture.whenStable();
    q(element, 'clear-filters')?.click();
    expect(api.list).toHaveBeenLastCalledWith(0, 20, {});
  });

  it('says so when nothing matches, or when there is nothing yet', async () => {
    const { fixture, element } = await open();
    api.list.mockReturnValue(of({ ...ENTRIES, content: [], totalElements: 0, totalPages: 0 }));

    (q(element, 'status-filter') as HTMLSelectElement).value = 'REVERSED';
    q(element, 'status-filter')?.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(text(element, 'empty')).toBe('No entry matches these filters.');
  });

  it('offers a retry when loading fails', async () => {
    const { fixture, element } = await open();
    api.list.mockReturnValueOnce(throwError(() => new Error('down')));
    q(element, 'status-filter')?.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(q(element, 'error')).not.toBeNull();

    (q(element, 'error')?.querySelector('button') as HTMLButtonElement).click();
    await fixture.whenStable();
    expect(q(element, 'error')).toBeNull();
  });
});
