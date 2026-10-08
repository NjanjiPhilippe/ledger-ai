import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { AppIcon, IconName } from '../../../shared/ui/icon';

/** Three figures drawn from the balances: assets, liabilities and equity, and the net result. */
@Component({
  selector: 'app-balance-summary',
  imports: [TranslocoPipe, MoneyPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="grid grid-cols-1 gap-4 md:grid-cols-3">
      @for (card of cards(); track card.key) {
        <article
          class="flex items-start gap-4 rounded-2xl border border-mist bg-white p-5 shadow-card"
        >
          <span
            class="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-fog text-ink"
          >
            <app-icon [name]="card.icon" class="size-5" />
          </span>
          <div class="min-w-0">
            <p class="text-xs font-medium uppercase tracking-wider text-graphite">
              {{ 'balance.summary.' + card.key + '.label' | transloco }}
            </p>
            <p
              class="mt-0.5 text-lg font-bold tabular-nums"
              [class.text-negative]="card.value.startsWith('-')"
              [attr.data-testid]="'summary-' + card.key"
            >
              {{ card.value | money: currency() }}
            </p>
            <p class="mt-1 text-xs leading-normal text-graphite">
              {{ 'balance.summary.' + card.key + '.hint' | transloco }}
            </p>
          </div>
        </article>
      }
    </div>
  `,
})
export class BalanceSummary {
  readonly assets = input.required<string>();
  readonly liabilitiesAndEquity = input.required<string>();
  readonly netResult = input.required<string>();
  readonly currency = input.required<string>();

  protected cards(): { key: string; icon: IconName; value: string }[] {
    return [
      { key: 'assets', icon: 'wallet', value: this.assets() },
      { key: 'liabilitiesAndEquity', icon: 'receipt', value: this.liabilitiesAndEquity() },
      { key: 'netResult', icon: 'trendingUp', value: this.netResult() },
    ];
  }
}
