import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { translocoTesting } from '../../../testing/transloco';
import { AuthService } from '../auth/auth.service';
import { LanguageService } from '../i18n/language.service';
import { Shell } from './shell';

describe('Shell', () => {
  const logout = vi.fn();

  async function render(roles: string[] = ['viewer']) {
    logout.mockReset();
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Shell, translocoTesting()],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            displayName: signal('Vera Viewer'),
            appRoles: signal(roles),
            logout,
          },
        },
      ],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const links = (element: HTMLElement) =>
    Array.from(element.querySelectorAll('aside nav a')).map((a) => a.textContent?.trim());

  it('shows the brand, the sidebar navigation and who is signed in, by name', async () => {
    const { element } = await render();

    expect(element.querySelector('[data-testid="brand"]')?.textContent?.trim()).toBe('LedgerAI');
    expect(links(element)).toEqual(['Dashboard', 'Accounts']);
    expect(element.querySelector('[data-testid="user-name"]')?.textContent?.trim()).toBe(
      'Vera Viewer',
    );
    expect(element.querySelector('[data-testid="user-role"]')?.textContent?.trim()).toBe('Viewer');
  });

  it('offers "New entry" only to an accountant or an administrator', async () => {
    expect(links((await render(['viewer'])).element)).not.toContain('New entry');
  });

  it('shows "New entry" to an accountant', async () => {
    TestBed.resetTestingModule();
    expect(links((await render(['accountant'])).element)).toEqual([
      'Dashboard',
      'Accounts',
      'New entry',
    ]);
  });

  it('opens and closes the menu on small screens', async () => {
    const { fixture, element } = await render();
    const sidebar = element.querySelector('#sidebar') as HTMLElement;
    const open = element.querySelector('header button') as HTMLButtonElement;

    expect(sidebar.classList).toContain('-translate-x-full');
    expect(open.getAttribute('aria-expanded')).toBe('false');

    open.click();
    await fixture.whenStable();
    expect(sidebar.classList).not.toContain('-translate-x-full');
    expect(open.getAttribute('aria-expanded')).toBe('true');

    (element.querySelector('aside nav a') as HTMLAnchorElement).click();
    await fixture.whenStable();
    expect(sidebar.classList).toContain('-translate-x-full');
  });

  it('signs the user out', async () => {
    const { element } = await render();

    const button = Array.from(element.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Sign out',
    ) as HTMLButtonElement;
    button.click();

    expect(logout).toHaveBeenCalledTimes(1);
  });

  it('switches the whole interface to French from the language switcher', async () => {
    const { fixture, element } = await render();

    (element.querySelector('button[lang="fr"]') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(links(element)).toEqual(['Tableau de bord', 'Comptes']);
    expect(element.querySelector('[data-testid="user-role"]')?.textContent?.trim()).toBe('Lecteur');
    expect(element.querySelector('button[lang="fr"]')?.getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('button[lang="en"]')?.getAttribute('aria-pressed')).toBe('false');
  });
});
