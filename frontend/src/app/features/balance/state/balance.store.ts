import { computed, inject, Injectable, signal } from '@angular/core';
import { AccountType, BalanceGroup, TrialBalance } from '../../../core/api/api-types';
import {
  Decimal,
  isZero,
  parseDecimal,
  subtract,
  sum,
  toDecimalString,
  abs,
} from '../../../shared/money/decimal';
import { BalanceApi } from '../data-access/balance.api';

export type BalanceStatus = 'loading' | 'loaded' | 'error';

export const TYPE_ORDER: readonly AccountType[] = [
  'ASSET',
  'LIABILITY',
  'EQUITY',
  'REVENUE',
  'EXPENSE',
];

const dec = (value: string): Decimal => parseDecimal(value) ?? { units: 0n, scale: 0 };

/** State of the trial balance screen, provided by the route so that it starts fresh each time the screen opens. */
@Injectable()
export class BalanceStore {
  private readonly api = inject(BalanceApi);

  readonly status = signal<BalanceStatus>('loading');
  readonly report = signal<TrialBalance | null>(null);

  /** Accounts without any movement are noise on a balance: hidden unless asked for. */
  readonly hideEmpty = signal(true);
  readonly search = signal('');

  private readonly visibleLines = computed(() => {
    const report = this.report();
    const needle = this.search().trim().toLowerCase();
    return (report?.lines ?? []).filter(
      (line) =>
        (!this.hideEmpty() || !(isZero(dec(line.totalDebits)) && isZero(dec(line.totalCredits)))) &&
        (needle === '' || line.accountName.toLowerCase().includes(needle)),
    );
  });

  readonly groups = computed<BalanceGroup[]>(() =>
    TYPE_ORDER.map((type) => {
      const lines = this.visibleLines().filter((l) => l.accountType === type);
      return {
        type,
        lines,
        totalDebits: toDecimalString(sum(lines.map((l) => dec(l.totalDebits)))),
        totalCredits: toDecimalString(sum(lines.map((l) => dec(l.totalCredits)))),
      };
    }).filter((group) => group.lines.length > 0),
  );

  readonly shownCount = computed(() => this.visibleLines().length);
  readonly hiddenCount = computed(
    () => (this.report()?.lines.length ?? 0) - this.visibleLines().length,
  );
  readonly movementCount = computed(
    () =>
      (this.report()?.lines ?? []).filter(
        (l) => !(isZero(dec(l.totalDebits)) && isZero(dec(l.totalCredits))),
      ).length,
  );

  /** Debits minus credits, as the absolute difference: zero when the ledger is in balance. */
  readonly gap = computed(() => {
    const report = this.report();
    return report
      ? toDecimalString(abs(subtract(dec(report.totalDebits), dec(report.totalCredits))))
      : '0';
  });

  private balanceOf(type: AccountType): Decimal {
    return sum(
      (this.report()?.lines ?? []).filter((l) => l.accountType === type).map((l) => dec(l.balance)),
    );
  }

  /** The accounting equation, from the balances (each shown on the normal side of its type). */
  readonly summary = computed(() => ({
    assets: toDecimalString(this.balanceOf('ASSET')),
    liabilitiesAndEquity: toDecimalString(
      sum([this.balanceOf('LIABILITY'), this.balanceOf('EQUITY')]),
    ),
    netResult: toDecimalString(subtract(this.balanceOf('REVENUE'), this.balanceOf('EXPENSE'))),
  }));

  load(): void {
    this.status.set('loading');
    this.api.trialBalance().subscribe({
      next: (report) => {
        this.report.set(report);
        this.status.set('loaded');
      },
      // Already toasted by the interceptor: the screen offers a retry.
      error: () => this.status.set('error'),
    });
  }
}
