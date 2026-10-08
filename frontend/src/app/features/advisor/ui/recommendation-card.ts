import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AdviceCategory, Severity } from '../../../core/api/api-types';
import { AppIcon, IconName } from '../../../shared/ui/icon';

const SEVERITY_CLASSES: Record<Severity, string> = {
  CRITICAL: 'bg-negative-soft text-negative',
  WARNING: 'bg-caution-soft text-caution',
  INFO: 'bg-fog text-graphite',
};

const SEVERITY_ICONS: Record<Severity, IconName> = {
  CRITICAL: 'alertTriangle',
  WARNING: 'alertTriangle',
  INFO: 'info',
};

const CATEGORY_ICONS: Record<AdviceCategory, IconName> = {
  LIQUIDITY: 'droplet',
  PROFITABILITY: 'trendingUp',
  RISK: 'shieldAlert',
  COMPLIANCE: 'shield',
  GROWTH: 'barChart',
  GENERAL: 'info',
};

/** One recommendation: how serious it is, what it is about, and what the advisor says. Presentation only. */
@Component({
  selector: 'app-recommendation-card',
  imports: [TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <article
      class="flex h-full flex-col gap-3 rounded-2xl border border-mist bg-white p-5 shadow-card"
      data-testid="recommendation"
    >
      <header class="flex flex-wrap items-center justify-between gap-2 border-b border-mist pb-3">
        <span
          class="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide"
          [class]="severityClasses()"
          data-testid="severity"
        >
          <app-icon [name]="severityIcon()" class="size-3.5" />
          {{ 'advisor.severity.' + severity() | transloco }}
        </span>
        <span
          class="inline-flex items-center gap-1.5 rounded-full bg-fog px-2.5 py-1 text-xs font-medium text-graphite"
          data-testid="category"
        >
          <app-icon [name]="categoryIcon()" class="size-3.5" />
          {{ 'advisor.category.' + category() | transloco }}
        </span>
      </header>
      <h3 class="text-lg font-bold tracking-tight" data-testid="title">{{ title() }}</h3>
      <p class="whitespace-pre-line text-sm leading-relaxed text-graphite" data-testid="detail">
        {{ detail() }}
      </p>
    </article>
  `,
})
export class RecommendationCard {
  readonly severity = input.required<Severity>();
  readonly category = input.required<AdviceCategory>();
  readonly title = input.required<string>();
  readonly detail = input.required<string>();

  protected severityClasses(): string {
    return SEVERITY_CLASSES[this.severity()];
  }

  protected severityIcon(): IconName {
    return SEVERITY_ICONS[this.severity()];
  }

  protected categoryIcon(): IconName {
    return CATEGORY_ICONS[this.category()];
  }
}
