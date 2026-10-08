import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { AppIcon } from '../../../shared/ui/icon';
import { BalanceStore } from '../state/balance.store';
import { trialBalanceToCsv } from '../state/balance-csv';
import { BalanceSummary } from '../ui/balance-summary';
import { BalanceTable } from '../ui/balance-table';

@Component({
  selector: 'app-balance-page',
  imports: [TranslocoPipe, ShortDatePipe, MoneyPipe, AppIcon, BalanceTable, BalanceSummary],
  providers: [BalanceStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="text-3xl font-bold tracking-tight">{{ 'balance.title' | transloco }}</h1>
          @if (store.report(); as report) {
            <p class="mt-1 text-graphite" data-testid="generated">
              {{ 'balance.generatedAt' | transloco }} {{ report.generatedAt | shortDate: 'long' }}
            </p>
          }
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-full border border-silver bg-white px-5 py-2.5 text-sm font-medium hover:bg-fog disabled:opacity-50"
          [disabled]="!store.report()"
          (click)="exportCsv()"
          data-testid="export"
        >
          <app-icon name="download" class="size-4" />
          {{ 'balance.export' | transloco }}
        </button>
      </header>

      @switch (store.status()) {
        @case ('error') {
          <div
            class="rounded-2xl bg-negative-soft p-4 text-negative"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'balance.loadFailed' | transloco }}</p>
            <button
              type="button"
              class="mt-2 rounded-lg bg-ink px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-soft"
              (click)="store.load()"
            >
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @case ('loaded') {
          @if (store.report(); as report) {
            <section
              class="grid grid-cols-1 gap-4 lg:grid-cols-[1.4fr_1fr_1fr_1fr]"
              [attr.aria-label]="'dashboard.kpi.label' | transloco"
            >
              <div
                class="flex items-start gap-3 rounded-2xl border p-5 shadow-card"
                [class]="
                  report.balanced
                    ? 'border-positive/30 bg-positive-soft text-positive'
                    : 'border-negative/30 bg-negative-soft text-negative'
                "
                role="status"
                data-testid="balance-status"
              >
                <app-icon
                  [name]="report.balanced ? 'checkCircle' : 'alertTriangle'"
                  class="mt-0.5 size-6 shrink-0"
                />
                <div>
                  <p class="font-bold">
                    {{ (report.balanced ? 'balance.balanced' : 'balance.unbalanced') | transloco }}
                  </p>
                  <p class="mt-1 text-sm opacity-90">
                    {{
                      (report.balanced ? 'balance.balancedHint' : 'balance.unbalancedHint')
                        | transloco
                    }}
                  </p>
                  <p class="mt-2 text-xs opacity-80">
                    {{
                      'balance.counts'
                        | transloco: { moved: store.movementCount(), total: report.lines.length }
                    }}
                  </p>
                </div>
              </div>
              <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                  {{ 'dashboard.kpi.debits' | transloco }}
                </p>
                <p class="mt-1 text-xl font-bold tabular-nums" data-testid="total-debits">
                  {{ report.totalDebits | money: report.currencyCode }}
                </p>
              </div>
              <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                  {{ 'dashboard.kpi.credits' | transloco }}
                </p>
                <p class="mt-1 text-xl font-bold tabular-nums" data-testid="total-credits">
                  {{ report.totalCredits | money: report.currencyCode }}
                </p>
              </div>
              <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                  {{ 'entry.totals.gap' | transloco }}
                </p>
                <p
                  class="mt-1 text-xl font-bold tabular-nums"
                  [class]="report.balanced ? 'text-positive' : 'text-negative'"
                  data-testid="gap"
                >
                  {{ store.gap() | money: report.currencyCode }}
                </p>
              </div>
            </section>

            <section class="overflow-hidden rounded-2xl border border-mist bg-white shadow-card">
              <div class="flex flex-wrap items-center gap-4 border-b border-mist p-4">
                <div class="relative min-w-56 flex-1 sm:max-w-sm">
                  <app-icon
                    name="search"
                    class="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-steel"
                  />
                  <input
                    type="search"
                    class="w-full rounded-full border border-silver bg-white py-2.5 pl-10 pr-4 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
                    [attr.aria-label]="'accounts.filters.search' | transloco"
                    [placeholder]="'accounts.filters.search' | transloco"
                    [value]="store.search()"
                    (input)="store.search.set($any($event.target).value)"
                    data-testid="search"
                  />
                </div>
                <label class="flex cursor-pointer items-center gap-2 text-sm text-graphite">
                  <input
                    type="checkbox"
                    class="size-4 accent-ink"
                    [checked]="store.hideEmpty()"
                    (change)="store.hideEmpty.set($any($event.target).checked)"
                    data-testid="hide-empty"
                  />
                  {{ 'balance.hideEmpty' | transloco }}
                </label>
                @if (store.hiddenCount() > 0) {
                  <span class="text-xs text-graphite" data-testid="hidden-count">
                    {{ 'balance.hidden' | transloco: { count: store.hiddenCount() } }}
                  </span>
                }
              </div>

              @if (store.groups().length === 0) {
                <div
                  class="flex flex-col items-center gap-2 p-10 text-center text-graphite"
                  data-testid="empty"
                >
                  <app-icon name="inbox" class="size-8" />
                  <p>{{ 'balance.empty' | transloco }}</p>
                </div>
              } @else {
                <app-balance-table
                  [groups]="store.groups()"
                  [currency]="report.currencyCode"
                  [totalDebits]="report.totalDebits"
                  [totalCredits]="report.totalCredits"
                  [balanced]="report.balanced"
                />
              }
            </section>

            <app-balance-summary
              [assets]="store.summary().assets"
              [liabilitiesAndEquity]="store.summary().liabilitiesAndEquity"
              [netResult]="store.summary().netResult"
              [currency]="report.currencyCode"
            />
          }
        }
        @default {
          <p class="text-graphite" role="status" data-testid="loading">
            {{ 'common.loading' | transloco }}
          </p>
        }
      }
    </div>
  `,
})
export class BalancePage implements OnInit {
  protected readonly store = inject(BalanceStore);
  private readonly transloco = inject(TranslocoService);

  ngOnInit(): void {
    this.store.load();
  }

  /** Downloads every account, not only the ones shown: the file is the whole report. */
  protected exportCsv(): void {
    const report = this.store.report();
    if (!report) {
      return;
    }
    const t = (key: string) => this.transloco.translate(key);
    const csv = trialBalanceToCsv(report, {
      account: t('dashboard.trialBalance.columns.account'),
      type: t('accounts.columns.type'),
      debits: t('dashboard.trialBalance.columns.debit'),
      credits: t('dashboard.trialBalance.columns.credit'),
      balance: t('dashboard.trialBalance.columns.balance'),
      total: t('dashboard.trialBalance.total'),
      typeOf: (type) => t(`accountType.${type}`),
    });
    // The byte order mark lets Excel read accents correctly.
    const blob = new Blob(['﻿', csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `trial-balance-${report.generatedAt.slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }
}
