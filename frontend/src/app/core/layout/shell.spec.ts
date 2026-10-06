import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { translocoTesting } from '../../../testing/transloco';
import { AuthService } from '../auth/auth.service';
import { LanguageService } from '../i18n/language.service';
import { Shell } from './shell';

describe('Shell', () => {
  const logout = vi.fn();

  async function render() {
    logout.mockReset();
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [Shell, translocoTesting()],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { username: signal('viewer'), logout } },
      ],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(Shell);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('shows the brand, the navigation and who is signed in', async () => {
    const { element } = await render();

    expect(element.querySelector('header a')?.textContent?.trim()).toBe('LedgerAI');
    expect(Array.from(element.querySelectorAll('nav a')).map((a) => a.textContent?.trim())).toEqual(
      ['Home', 'Accounts'],
    );
    expect(element.querySelector('[data-testid="signed-in-as"]')?.textContent?.trim()).toBe(
      'Signed in as viewer',
    );
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

    expect(Array.from(element.querySelectorAll('nav a')).map((a) => a.textContent?.trim())).toEqual(
      ['Accueil', 'Comptes'],
    );
    expect(element.querySelector('[data-testid="signed-in-as"]')?.textContent?.trim()).toBe(
      'Connecté en tant que viewer',
    );
    expect(element.querySelector('button[lang="fr"]')?.getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('button[lang="en"]')?.getAttribute('aria-pressed')).toBe('false');
  });
});
