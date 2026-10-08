import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ACCOUNTS, DRAFT_ENTRY, ENTRY, REVERSAL_ENTRY } from '../../../../testing/ledger';
import { translocoTesting } from '../../../../testing/transloco';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { ToastService } from '../../../core/toast/toast.service';
import { EntriesApi } from '../data-access/entries.api';
import { EntryDetailPage } from './entry-detail-page';

describe('EntryDetailPage', () => {
  const api = { get: vi.fn(), accounts: vi.fn(), post: vi.fn(), reverse: vi.fn() };
  const navigate = vi.fn();

  async function open(entry = ENTRY, roles: string[] = ['viewer']) {
    localStorage.clear();
    api.get.mockReset().mockImplementation((id: string) => of(id === entry.id ? entry : ENTRY));
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
    api.post.mockReset();
    api.reverse.mockReset();
    navigate.mockReset();
    const params = new BehaviorSubject({ get: () => entry.id });
    TestBed.configureTestingModule({
      imports: [EntryDetailPage, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: EntriesApi, useValue: api },
        { provide: AuthService, useValue: { appRoles: signal(roles) } },
        { provide: ActivatedRoute, useValue: { paramMap: params } },
      ],
    });
    TestBed.inject(LanguageService).initialize();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockImplementation(navigate);
    const fixture = TestBed.createComponent(EntryDetailPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const q = (element: HTMLElement, id: string) =>
    element.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const text = (element: HTMLElement, id: string) =>
    q(element, id)?.textContent?.replace(/\s+/g, ' ').trim();

  it('shows the entry with account names, amounts per side and balanced totals', async () => {
    const { element } = await open();

    expect(text(element, 'title')).toBe('First sale');
    expect(text(element, 'entry-status')).toBe('Posted');
    const accounts = Array.from(element.querySelectorAll('[data-testid="line-account"]')).map((e) =>
      e.textContent?.trim(),
    );
    expect(accounts).toEqual(['Cash', 'Sales']);
    expect(text(element, 'line-debit')).toMatch(/2,450,000/);
    expect(text(element, 'total-credits')).toMatch(/2,450,000/);
    expect(element.textContent).toContain('Balanced');
    expect(element.textContent).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-/);
  });

  it('offers no action to a viewer', async () => {
    const { element } = await open(ENTRY, ['viewer']);

    expect(q(element, 'post')).toBeNull();
    expect(q(element, 'reverse')).toBeNull();
  });

  it('lets an accountant post a draft, after a confirmation', async () => {
    const { fixture, element } = await open(DRAFT_ENTRY, ['accountant']);
    api.post.mockReturnValue(of({ ...DRAFT_ENTRY, status: 'POSTED' }));
    expect(q(element, 'reverse')).toBeNull();

    q(element, 'post')?.click();
    await fixture.whenStable();
    expect(q(element, 'confirm-dialog')).not.toBeNull();
    expect(api.post).not.toHaveBeenCalled();

    q(element, 'confirm-ok')?.click();
    await fixture.whenStable();

    expect(api.post).toHaveBeenCalledWith(DRAFT_ENTRY.id);
    expect(q(element, 'confirm-dialog')).toBeNull();
    expect(text(element, 'entry-status')).toBe('Posted');
    expect(q(element, 'post')).toBeNull();
    expect(TestBed.inject(ToastService).toasts()[0].title).toBe('Entry "October rent" posted');
  });

  it('does nothing when the confirmation is dismissed', async () => {
    const { fixture, element } = await open(DRAFT_ENTRY, ['accountant']);

    q(element, 'post')?.click();
    await fixture.whenStable();
    q(element, 'confirm-cancel')?.click();
    await fixture.whenStable();

    expect(api.post).not.toHaveBeenCalled();
    expect(q(element, 'confirm-dialog')).toBeNull();
  });

  it('lets an administrator reverse a posted entry, and opens the reversal', async () => {
    const { fixture, element } = await open(ENTRY, ['admin']);
    api.reverse.mockReturnValue(of(REVERSAL_ENTRY));
    expect(q(element, 'post')).toBeNull();

    q(element, 'reverse')?.click();
    await fixture.whenStable();
    q(element, 'confirm-ok')?.click();
    await fixture.whenStable();

    expect(api.reverse).toHaveBeenCalledWith(ENTRY.id);
    expect(navigate).toHaveBeenCalledWith(['/entries', REVERSAL_ENTRY.id]);
    expect(TestBed.inject(ToastService).toasts()[0].title).toBe('Entry "First sale" reversed');
  });

  it('only offers to reverse a posted entry, never a reversed one', async () => {
    const { element } = await open({ ...ENTRY, status: 'REVERSED' }, ['admin']);

    expect(q(element, 'reverse')).toBeNull();
    expect(q(element, 'post')).toBeNull();
  });

  it('keeps the page and closes the question when the server refuses', async () => {
    const { fixture, element } = await open(ENTRY, ['admin']);
    api.reverse.mockReturnValue(throwError(() => new Error('409')));

    q(element, 'reverse')?.click();
    await fixture.whenStable();
    q(element, 'confirm-ok')?.click();
    await fixture.whenStable();

    expect(navigate).not.toHaveBeenCalled();
    expect(q(element, 'confirm-dialog')).toBeNull();
    expect(q(element, 'title')).not.toBeNull();
  });

  it('links a reversal to its original, by name', async () => {
    const { element } = await open(REVERSAL_ENTRY);

    expect(text(element, 'original')).toBe('First sale');
    expect(q(element, 'original')?.getAttribute('href')).toBe(`/entries/${ENTRY.id}`);
  });

  it('says so when the entry does not exist', async () => {
    api.get.mockReset().mockReturnValue(throwError(() => ({ status: 404 })));
    TestBed.resetTestingModule();
    const params = new BehaviorSubject({ get: () => 'nope' });
    api.accounts.mockReturnValue(of(ACCOUNTS));
    TestBed.configureTestingModule({
      imports: [EntryDetailPage, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: EntriesApi, useValue: api },
        { provide: AuthService, useValue: { appRoles: signal([]) } },
        { provide: ActivatedRoute, useValue: { paramMap: params } },
      ],
    });
    const fixture = TestBed.createComponent(EntryDetailPage);
    await fixture.whenStable();

    expect(q(fixture.nativeElement, 'not-found')).not.toBeNull();
  });
});
