import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { AppIcon } from '../../../shared/ui/icon';

/** Debits, credits and the difference, live. Presentation only: the amounts arrive as exact decimal strings. */
@Component({
  selector: 'app-entry-totals',
  imports: [TranslocoPipe, MoneyPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
      <div class="rounded-2xl bg-fog p-4">
        <p class="text-xs font-medium uppercase tracking-wider text-graphite">
          {{ 'entry.totals.debits' | transloco }}
        </p>
        <p class="mt-1 text-xl font-bold tabular-nums" data-testid="total-debits">
          {{ debits() | money: currency() }}
        </p>
      </div>
      <div class="rounded-2xl bg-fog p-4">
        <p class="text-xs font-medium uppercase tracking-wider text-graphite">
          {{ 'entry.totals.credits' | transloco }}
        </p>
        <p class="mt-1 text-xl font-bold tabular-nums" data-testid="total-credits">
          {{ credits() | money: currency() }}
        </p>
      </div>
      <div
        class="rounded-2xl p-4"
        [class]="balanced() ? 'bg-positive-soft text-positive' : 'bg-negative-soft text-negative'"
        role="status"
        data-testid="balance-status"
      >
        <p class="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wider">
          <app-icon [name]="balanced() ? 'checkCircle' : 'alertTriangle'" class="size-3.5" />
          {{ 'entry.totals.gap' | transloco }}
        </p>
        <p class="mt-1 text-xl font-bold tabular-nums">
          @if (balanced()) {
            {{ 'entry.totals.balanced' | transloco }}
          } @else {
            {{ gap() | money: currency() }}
          }
        </p>
      </div>
    </div>
  `,
})
export class EntryTotals {
  readonly debits = input.required<string>();
  readonly credits = input.required<string>();
  readonly gap = input.required<string>();
  readonly balanced = input.required<boolean>();
  readonly currency = input.required<string>();
}
