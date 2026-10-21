import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from '../../../core/auth/auth.service';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { AppIcon } from '../../../shared/ui/icon';
import { Panel } from '../../../shared/ui/panel';
import { DashboardStore } from '../state/dashboard.store';
import { AccountTypesList } from '../ui/account-types-list';
import { KpiCard } from '../ui/kpi-card';
import { RecentEntriesList } from '../ui/recent-entries-list';
import { TrialBalanceTable } from '../ui/trial-balance-table';

/** The landing page after sign-in: where the ledger stands, with names instead of identifiers. */
@Component({
  selector: 'app-dashboard-page',
  imports: [
    RouterLink,
    TranslocoPipe,
    ShortDatePipe,
    MoneyPipe,
    AppIcon,
    Panel,
    KpiCard,
    TrialBalanceTable,
    AccountTypesList,
    RecentEntriesList,
  ],
  providers: [DashboardStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-sm text-graphite" data-testid="today">{{ today | shortDate: 'long' }}</p>
          <h1 class="mt-1 text-3xl font-bold tracking-tight">
            {{ 'dashboard.welcome' | transloco: { name: auth.displayName() ?? '' } }}
          </h1>
          <p class="mt-1 text-graphite">{{ 'dashboard.subtitle' | transloco }}</p>
        </div>
        <ul
          class="flex flex-wrap gap-2"
          data-testid="roles"
          [attr.aria-label]="'dashboard.roles' | transloco"
        >
          @for (role of auth.appRoles(); track role) {
            <li
              class="inline-flex items-center gap-1.5 rounded-full border border-mist bg-white px-3 py-1 text-sm font-medium shadow-card"
            >
              <app-icon name="shield" class="size-3.5 text-graphite" />
              {{ 'roles.' + role | transloco }}
            </li>
          } @empty {
            <li class="text-sm text-graphite">{{ 'dashboard.noRoles' | transloco }}</li>
          }
        </ul>
      </header>

      <section
        class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
        [attr.aria-label]="'dashboard.kpi.label' | transloco"
      >
        <app-kpi-card
          icon="landmark"
          [label]="'dashboard.kpi.accounts' | transloco"
          [value]="'' + store.totalAccounts()"
          [hint]="
            'dashboard.kpi.accountsHint'
              | transloco: { active: store.activeAccounts(), total: store.totalAccounts() }
          "
        />
        @let report = store.trialBalance().data;
        <app-kpi-card
          icon="arrowUpRight"
          [label]="'dashboard.kpi.debits' | transloco"
          [value]="report ? (report.totalDebits | money: report.currencyCode) : '–'"
          [hint]="'dashboard.kpi.debitsHint' | transloco"
        />
        <app-kpi-card
          icon="arrowDownLeft"
          [label]="'dashboard.kpi.credits' | transloco"
          [value]="report ? (report.totalCredits | money: report.currencyCode) : '–'"
          [hint]="'dashboard.kpi.creditsHint' | transloco"
        />
        <app-kpi-card
          [icon]="report && !report.balanced ? 'alertTriangle' : 'checkCircle'"
          [tone]="!report ? 'neutral' : report.balanced ? 'positive' : 'negative'"
          [label]="'dashboard.kpi.ledger' | transloco"
          [value]="
            !report
              ? '–'
              : ((report.balanced ? 'dashboard.kpi.balanced' : 'dashboard.kpi.unbalanced')
                | transloco)
          "
          [hint]="
            !report
              ? ''
              : ((report.balanced ? 'dashboard.kpi.balancedHint' : 'dashboard.kpi.unbalancedHint')
                | transloco)
          "
        />
      </section>

      <div class="grid grid-cols-1 gap-5 xl:grid-cols-12">
        <div class="xl:col-span-8">
          <app-panel
            icon="book"
            [title]="'dashboard.trialBalance.title' | transloco"
            [status]="store.trialBalance().status"
            (retry)="store.loadTrialBalance()"
          >
            @if (store.trialBalance().data; as data) {
              <app-trial-balance-table [report]="data" />
            }
          </app-panel>
        </div>
        <div class="xl:col-span-4">
          <app-panel
            icon="pie"
            [title]="'dashboard.types.title' | transloco"
            [status]="store.accounts().status"
            (retry)="store.loadAccounts()"
          >
            <app-account-types-list [counts]="store.accountsByType()" />
          </app-panel>
        </div>
      </div>

      <app-panel
        icon="clock"
        [title]="'dashboard.entries.title' | transloco"
        [status]="entriesStatus()"
        (retry)="retryEntries()"
      >
        <a
          panel-actions
          routerLink="/entries"
          class="rounded-full px-3 py-1.5 text-sm font-medium text-graphite hover:bg-fog hover:text-ink"
          data-testid="view-all"
        >
          {{ 'dashboard.entries.viewAll' | transloco }}
        </a>
        @if (store.entries().data; as page) {
          <app-recent-entries-list
            [entries]="page.content"
            [accountNames]="store.accountNames()"
            [unknownAccount]="'common.unknownAccount' | transloco"
          />
        }
      </app-panel>
    </div>
  `,
})
export class DashboardPage implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly store = inject(DashboardStore);
  protected readonly today = new Date();

  ngOnInit(): void {
    this.store.load();
  }

  /** Entries are worded with account names: the page waits for the accounts too before showing them. */
  protected entriesStatus() {
    const entries = this.store.entries().status;
    const accounts = this.store.accounts().status;
    if (entries === 'error') {
      return 'error';
    }
    return entries === 'loaded' && accounts !== 'loading' ? 'loaded' : 'loading';
  }

  protected retryEntries(): void {
    this.store.loadEntries();
    if (this.store.accounts().status === 'error') {
      this.store.loadAccounts();
    }
  }
}
