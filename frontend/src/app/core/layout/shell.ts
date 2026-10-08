import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcon, IconName } from '../../shared/ui/icon';
import { AuthService } from '../auth/auth.service';
import { LanguageSwitcher } from '../i18n/language-switcher';

interface NavItem {
  readonly path: string;
  readonly labelKey: string;
  readonly icon: IconName;
  readonly exact: boolean;
}

/** The frame of every signed-in screen: a sidebar (a drawer on small screens) and the outlet of the current page. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslocoPipe, LanguageSwitcher, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="min-h-screen lg:grid lg:grid-cols-[19rem_minmax(0,1fr)]">
      <header
        class="sticky top-0 z-30 flex items-center gap-3 border-b border-mist bg-white px-4 py-3 lg:hidden"
      >
        <button
          type="button"
          class="rounded-lg p-2 text-ink hover:bg-fog focus-visible:outline-2 focus-visible:outline-mint"
          [attr.aria-label]="'nav.openMenu' | transloco"
          [attr.aria-expanded]="menuOpen()"
          aria-controls="sidebar"
          (click)="menuOpen.set(true)"
        >
          <app-icon name="menu" class="size-5" />
        </button>
        <span class="font-bold tracking-tight">{{ 'app.name' | transloco }}</span>
      </header>

      @if (menuOpen()) {
        <button
          type="button"
          class="fixed inset-0 z-30 bg-ink/40 lg:hidden"
          [attr.aria-label]="'nav.closeMenu' | transloco"
          (click)="menuOpen.set(false)"
        ></button>
      }

      <div
        id="sidebar"
        class="fixed inset-y-0 left-0 z-40 w-[19rem] max-w-[90vw] p-3 transition-transform lg:sticky lg:top-0 lg:h-screen lg:max-w-none lg:translate-x-0 lg:p-4"
        [class.-translate-x-full]="!menuOpen()"
      >
        <aside
          class="flex h-full flex-col justify-between rounded-3xl border border-mist bg-white p-5 shadow-card"
          [attr.aria-label]="'nav.main' | transloco"
        >
          <div class="space-y-8">
            <div class="flex items-center justify-between gap-3 px-2 pt-1">
              <a routerLink="/" class="flex items-center gap-3" (click)="menuOpen.set(false)">
                <span class="flex size-9 items-center justify-center rounded-xl bg-ink shadow-card">
                  <svg viewBox="0 0 24 24" fill="none" class="size-5" aria-hidden="true">
                    <path
                      d="M4 6h12M4 12h9M4 18h12"
                      stroke="#fff"
                      stroke-width="2.5"
                      stroke-linecap="round"
                    />
                    <path
                      d="M14 9l4 3-4 3"
                      stroke="#31f2bf"
                      stroke-width="2.5"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </span>
                <span class="text-xl font-bold tracking-tight" data-testid="brand">{{
                  'app.name' | transloco
                }}</span>
              </a>
              <button
                type="button"
                class="rounded-lg p-1.5 text-graphite hover:bg-fog lg:hidden"
                [attr.aria-label]="'nav.closeMenu' | transloco"
                (click)="menuOpen.set(false)"
              >
                <app-icon name="close" class="size-5" />
              </button>
            </div>

            <nav class="space-y-1 text-sm font-medium" aria-label="main">
              @for (item of items; track item.path) {
                <a
                  [routerLink]="item.path"
                  routerLinkActive
                  #active="routerLinkActive"
                  [routerLinkActiveOptions]="{ exact: item.exact }"
                  [attr.aria-current]="active.isActive ? 'page' : null"
                  class="flex items-center gap-3 rounded-full px-4 py-2.5 transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-mint"
                  [class]="
                    active.isActive
                      ? 'bg-ink text-white shadow-card'
                      : 'text-graphite hover:bg-fog hover:text-ink'
                  "
                  (click)="menuOpen.set(false)"
                >
                  <app-icon
                    [name]="item.icon"
                    class="size-[18px]"
                    [class.text-mint]="active.isActive"
                  />
                  <span class="flex-1">{{ item.labelKey | transloco }}</span>
                  @if (active.isActive) {
                    <span class="size-2 rounded-full bg-mint shadow-[0_0_8px_#31f2bf]"></span>
                  }
                </a>
              }
            </nav>
          </div>

          <div class="space-y-3 border-t border-mist pt-4">
            <app-language-switcher />
            <div class="flex items-center gap-3 rounded-2xl bg-fog p-2.5">
              <span
                class="flex size-9 shrink-0 items-center justify-center rounded-full bg-white text-ink ring-1 ring-silver"
              >
                <app-icon name="user" class="size-[18px]" />
              </span>
              <div class="min-w-0">
                <p class="truncate text-sm font-bold" data-testid="user-name">
                  {{ auth.displayName() }}
                </p>
                <p class="truncate text-xs text-graphite" data-testid="user-role">
                  @for (role of auth.appRoles(); track role; let last = $last) {
                    {{ 'roles.' + role | transloco }}{{ last ? '' : ', ' }}
                  } @empty {
                    {{ 'dashboard.noRoles' | transloco }}
                  }
                </p>
              </div>
            </div>
            <button
              type="button"
              class="flex w-full items-center gap-2 rounded-full px-4 py-2 text-sm font-medium text-graphite hover:bg-fog hover:text-ink focus-visible:outline-2 focus-visible:outline-mint"
              (click)="auth.logout()"
            >
              <app-icon name="logout" class="size-[18px]" />
              {{ 'auth.logout' | transloco }}
            </button>
          </div>
        </aside>
      </div>

      <main class="min-w-0 px-4 py-6 sm:px-8 sm:py-8">
        <div class="mx-auto max-w-6xl">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly menuOpen = signal(false);

  protected readonly items: readonly NavItem[] = [
    { path: '/', labelKey: 'nav.dashboard', icon: 'dashboard', exact: true },
    { path: '/accounts', labelKey: 'nav.accounts', icon: 'landmark', exact: false },
  ];
}
