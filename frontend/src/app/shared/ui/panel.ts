import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcon, IconName } from './icon';

export type PanelStatus = 'loading' | 'loaded' | 'error';

/** A titled card that owns the loading and failure states of the data it shows, so every section behaves the same. */
@Component({
  selector: 'app-panel',
  imports: [AppIcon, TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="flex h-full flex-col rounded-2xl border border-mist bg-white p-5 shadow-card">
      <header class="mb-4 flex items-center gap-3">
        <span class="flex size-8 items-center justify-center rounded-lg bg-fog text-ink">
          <app-icon [name]="icon()" class="size-4" />
        </span>
        <h2 class="text-base font-bold tracking-tight">{{ title() }}</h2>
        <div class="ml-auto"><ng-content select="[panel-actions]" /></div>
      </header>

      @switch (status()) {
        @case ('loaded') {
          <ng-content />
        }
        @case ('error') {
          <div
            class="flex flex-1 flex-col items-center justify-center gap-3 rounded-xl bg-negative-soft p-6 text-center text-negative"
            role="alert"
            data-testid="panel-error"
          >
            <app-icon name="alertTriangle" class="size-6" />
            <p class="text-sm">{{ 'panel.failed' | transloco }}</p>
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg bg-ink px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-mint"
              (click)="retry.emit()"
            >
              <app-icon name="refresh" class="size-4" />
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @default {
          <div class="space-y-3" role="status" data-testid="panel-loading">
            <span class="sr-only">{{ 'common.loading' | transloco }}</span>
            <div class="h-4 w-2/3 animate-pulse rounded bg-fog"></div>
            <div class="h-4 w-full animate-pulse rounded bg-fog"></div>
            <div class="h-4 w-5/6 animate-pulse rounded bg-fog"></div>
          </div>
        }
      }
    </section>
  `,
})
export class Panel {
  readonly title = input.required<string>();
  readonly icon = input.required<IconName>();
  readonly status = input.required<PanelStatus>();
  readonly retry = output<void>();
}
