import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { translocoTesting } from '../../../../testing/transloco';
import { ACCOUNTS, ENTRIES, TRIAL_BALANCE } from '../../../../testing/ledger';
import { AuthService } from '../../../core/auth/auth.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { DashboardApi } from '../data-access/dashboard.api';
import { DashboardPage } from './dashboard-page';

describe('DashboardPage', () => {
  const api = { trialBalance: vi.fn(), recentEntries: vi.fn(), accounts: vi.fn() };

  async function open(roles: string[] = ['admin']) {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [DashboardPage, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: DashboardApi, useValue: api },
        {
          provide: AuthService,
          useValue: { displayName: signal('Ada Admin'), appRoles: signal(roles) },
        },
      ],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(DashboardPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const text = (element: HTMLElement, selector: string) =>
    element.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();

  beforeEach(() => {
    api.trialBalance.mockReset().mockReturnValue(of(TRIAL_BALANCE));
    api.recentEntries.mockReset().mockReturnValue(of(ENTRIES));
    api.accounts.mockReset().mockReturnValue(of(ACCOUNTS));
  });

  it('welcomes the user by name and lists their roles', async () => {
    const { element } = await open(['admin']);

    expect(text(element, 'h1')).toBe('Welcome back, Ada Admin');
    expect(text(element, '[data-testid="roles"]')).toBe('Administrator');
  });

  it('shows the key figures', async () => {
    const { element } = await open();

    const values = Array.from(element.querySelectorAll('[data-testid="kpi-value"]')).map((e) =>
      e.textContent?.replace(/\s+/g, ' ').trim(),
    );
    // The currency symbol and decimals come from the browser's ICU data (XAF is written "FCFA"): only check the figure.
    expect(values[0]).toBe('3');
    expect(values[1]).toMatch(/2,450,000/);
    expect(values[2]).toMatch(/2,450,000/);
    expect(values[3]).toBe('Balanced');
  });

  it('lists the trial balance and the recent entries with names, never identifiers', async () => {
    const { element } = await open();

    expect(element.querySelectorAll('[data-testid="trial-balance-row"]')).toHaveLength(2);
    expect(text(element, '[data-testid="entry-debited"]')).toBe('Cash');
    expect(text(element, '[data-testid="entry-credited"]')).toBe('Sales');
    expect(text(element, '[data-testid="entry-status"]')).toBe('Posted');
    expect(element.textContent).not.toMatch(/[0-9a-f]{8}-[0-9a-f]{4}-/);
  });

  it('flags a ledger that is out of balance', async () => {
    api.trialBalance.mockReturnValue(of({ ...TRIAL_BALANCE, balanced: false }));

    const { element } = await open();

    expect(
      Array.from(element.querySelectorAll('[data-testid="kpi-value"]')).at(-1)?.textContent?.trim(),
    ).toBe('Out of balance');
  });

  it('shows a retry in the section that failed, and only there', async () => {
    api.trialBalance.mockReturnValueOnce(throwError(() => new Error('down')));

    const { fixture, element } = await open();

    expect(element.querySelectorAll('[data-testid="panel-error"]')).toHaveLength(1);
    expect(element.querySelector('[data-testid="entry-row"]')).not.toBeNull();

    (element.querySelector('[data-testid="panel-error"] button') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="panel-error"]')).toBeNull();
    expect(element.querySelectorAll('[data-testid="trial-balance-row"]')).toHaveLength(2);
  });

  it('is entirely in French when the language is French', async () => {
    const { fixture, element } = await open();

    TestBed.inject(LanguageService).set('fr');
    await fixture.whenStable();

    expect(text(element, 'h1')).toBe('Bon retour, Ada Admin');
    expect(text(element, '[data-testid="entry-status"]')).toBe('Comptabilisée');
  });
});
