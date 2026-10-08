import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AppIcon, IconName } from '../../../shared/ui/icon';

export type KpiTone = 'neutral' | 'positive' | 'negative';

const TONE_CLASSES: Record<KpiTone, string> = {
  neutral: 'bg-fog text-ink',
  positive: 'bg-positive-soft text-positive',
  negative: 'bg-negative-soft text-negative',
};

/** One headline figure with its icon. Presentation only. */
@Component({
  selector: 'app-kpi-card',
  imports: [AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="flex h-full flex-col justify-between gap-4 rounded-2xl border border-mist bg-white p-5 shadow-card"
    >
      <div class="flex items-center justify-between gap-3">
        <span class="text-sm font-medium text-graphite">{{ label() }}</span>
        <span class="flex size-9 items-center justify-center rounded-xl" [class]="toneClasses()">
          <app-icon [name]="icon()" class="size-[18px]" />
        </span>
      </div>
      <div>
        <p class="text-2xl font-bold tracking-tight tabular-nums" data-testid="kpi-value">
          {{ value() }}
        </p>
        @if (hint()) {
          <p class="mt-1 text-xs text-graphite">{{ hint() }}</p>
        }
      </div>
    </div>
  `,
})
export class KpiCard {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly hint = input<string>();
  readonly icon = input.required<IconName>();
  readonly tone = input<KpiTone>('neutral');

  protected toneClasses(): string {
    return TONE_CLASSES[this.tone()];
  }
}
