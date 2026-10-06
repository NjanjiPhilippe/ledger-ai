import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from '../auth/auth.service';
import { LanguageSwitcher } from '../i18n/language-switcher';

/** The frame of every signed-in screen: brand, navigation, language, user and the outlet of the current page. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoPipe, LanguageSwitcher],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
        <a routerLink="/" class="text-lg font-semibold">{{ 'app.name' | transloco }}</a>
        <nav class="flex gap-4 text-sm" aria-label="main">
          <a
            routerLink="/"
            routerLinkActive="font-semibold underline"
            [routerLinkActiveOptions]="{ exact: true }"
          >
            {{ 'nav.home' | transloco }}
          </a>
          <a routerLink="/accounts" routerLinkActive="font-semibold underline">
            {{ 'nav.accounts' | transloco }}
          </a>
        </nav>
        <div class="ml-auto flex items-center gap-4">
          <app-language-switcher />
          <span class="hidden text-sm text-slate-600 sm:inline" data-testid="signed-in-as">
            {{ 'auth.signedInAs' | transloco: { name: auth.username() ?? '' } }}
          </span>
          <button
            type="button"
            class="rounded border border-slate-300 px-3 py-1 text-sm hover:bg-slate-100"
            (click)="auth.logout()"
          >
            {{ 'auth.logout' | transloco }}
          </button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-5xl px-4 py-6">
      <router-outlet />
    </main>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
}
