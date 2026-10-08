import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { AppIcon } from '../../../shared/ui/icon';
import { AdvisorStore } from '../state/advisor.store';
import { RecommendationCard } from '../ui/recommendation-card';

@Component({
  selector: 'app-advisor-page',
  imports: [TranslocoPipe, ShortDatePipe, AppIcon, RecommendationCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header
        class="flex flex-wrap items-center justify-between gap-6 rounded-2xl border border-mist bg-white p-6 shadow-card"
      >
        <div class="max-w-2xl space-y-2">
          <span
            class="inline-flex items-center gap-2 rounded-full bg-fog px-3 py-1 text-xs font-semibold text-graphite"
          >
            <app-icon name="sparkles" class="size-3.5 text-ink" />
            {{ 'advisor.badge' | transloco }}
          </span>
          <h1 class="text-3xl font-bold tracking-tight">{{ 'advisor.title' | transloco }}</h1>
          <p class="text-graphite">{{ 'advisor.subtitle' | transloco }}</p>
        </div>
        <div class="flex flex-col items-start gap-2 sm:items-end">
          <button
            type="button"
            class="inline-flex items-center gap-2 rounded-full bg-ink px-6 py-3 text-sm font-medium text-white shadow-card hover:bg-ink-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-mint disabled:opacity-70"
            [disabled]="store.status() === 'analyzing'"
            (click)="store.analyze()"
            data-testid="analyze"
          >
            <app-icon
              [name]="store.status() === 'analyzing' ? 'refresh' : 'sparkles'"
              class="size-4 text-mint"
              [class.animate-spin]="store.status() === 'analyzing'"
            />
            {{
              (store.status() === 'analyzing'
                ? 'advisor.analyzing'
                : store.advice()
                  ? 'advisor.analyzeAgain'
                  : 'advisor.analyze'
              ) | transloco
            }}
          </button>
          @if (store.advice(); as advice) {
            <p class="text-xs text-graphite" data-testid="meta">
              {{
                'advisor.generated'
                  | transloco: { date: (advice.generatedAt | shortDate), provider: advice.provider }
              }}
            </p>
          }
        </div>
      </header>

      @switch (store.status()) {
        @case ('idle') {
          <div
            class="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
            data-testid="idle"
          >
            <app-icon name="sparkles" class="size-8" />
            <p class="max-w-lg">{{ 'advisor.idle' | transloco }}</p>
            <p class="max-w-lg text-xs">{{ 'advisor.privacy' | transloco }}</p>
          </div>
        }
        @case ('error') {
          <div
            class="rounded-2xl bg-negative-soft p-4 text-negative"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'advisor.failed' | transloco }}</p>
          </div>
        }
        @default {
          @if (store.status() === 'analyzing' && !store.advice()) {
            <p class="text-graphite" role="status" data-testid="analyzing">
              {{ 'advisor.analyzingHint' | transloco }}
            </p>
          }
          @if (store.advice()) {
            @if (store.items().length === 0) {
              <div
                class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
                data-testid="nothing"
              >
                <app-icon name="checkCircle" class="size-8 text-positive" />
                <p>{{ 'advisor.nothing' | transloco }}</p>
              </div>
            } @else {
              <div class="flex flex-wrap items-center justify-between gap-4">
                <div
                  class="flex flex-wrap items-center gap-2"
                  role="group"
                  [attr.aria-label]="'advisor.filter' | transloco"
                >
                  <button
                    type="button"
                    class="rounded-full px-4 py-2 text-sm font-semibold"
                    [class]="
                      store.category() === 'ALL'
                        ? 'bg-ink text-white shadow-card'
                        : 'border border-silver bg-white text-graphite hover:text-ink'
                    "
                    [attr.aria-pressed]="store.category() === 'ALL'"
                    (click)="store.category.set('ALL')"
                    data-testid="filter-all"
                  >
                    {{ 'advisor.all' | transloco }} ({{ store.items().length }})
                  </button>
                  @for (entry of store.categories(); track entry.category) {
                    <button
                      type="button"
                      class="rounded-full px-4 py-2 text-sm font-semibold"
                      [class]="
                        store.category() === entry.category
                          ? 'bg-ink text-white shadow-card'
                          : 'border border-silver bg-white text-graphite hover:text-ink'
                      "
                      [attr.aria-pressed]="store.category() === entry.category"
                      (click)="store.category.set(entry.category)"
                      data-testid="filter-category"
                    >
                      {{ 'advisor.category.' + entry.category | transloco }} ({{ entry.count }})
                    </button>
                  }
                </div>
                <p
                  class="flex flex-wrap items-center gap-2 text-xs font-semibold"
                  data-testid="counts"
                >
                  <span class="rounded-full bg-negative-soft px-2.5 py-1 text-negative">
                    {{
                      'advisor.count.CRITICAL'
                        | transloco: { count: store.severityCounts().CRITICAL }
                    }}
                  </span>
                  <span class="rounded-full bg-caution-soft px-2.5 py-1 text-caution">
                    {{
                      'advisor.count.WARNING' | transloco: { count: store.severityCounts().WARNING }
                    }}
                  </span>
                  <span class="rounded-full bg-fog px-2.5 py-1 text-graphite">
                    {{ 'advisor.count.INFO' | transloco: { count: store.severityCounts().INFO } }}
                  </span>
                </p>
              </div>

              <div class="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
                @for (item of store.visibleItems(); track $index) {
                  <app-recommendation-card
                    [severity]="item.severity"
                    [category]="item.category"
                    [title]="item.title"
                    [detail]="item.detail"
                  />
                }
              </div>

              <p class="text-xs text-graphite" data-testid="disclaimer">
                {{ 'advisor.disclaimer' | transloco }}
              </p>
            }
          }
        }
      }
    </div>
  `,
})
export class AdvisorPage {
  protected readonly store = inject(AdvisorStore);
}
