import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** A yes/no question over the page. The caller decides what yes does: this only asks. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'dismissed.emit()' },
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        class="absolute inset-0 bg-ink/40"
        tabindex="-1"
        [attr.aria-label]="'common.cancel' | transloco"
        (click)="dismissed.emit()"
      ></button>
      <div
        class="relative w-full max-w-md space-y-4 rounded-3xl bg-white p-6 shadow-lift"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        aria-describedby="confirm-message"
        data-testid="confirm-dialog"
      >
        <h2 id="confirm-title" class="text-lg font-bold tracking-tight">{{ title() }}</h2>
        <p id="confirm-message" class="text-graphite">{{ message() }}</p>
        <div class="flex justify-end gap-2 pt-1">
          <button
            type="button"
            class="rounded-full border border-silver bg-white px-4 py-2 text-sm font-medium hover:bg-fog"
            (click)="dismissed.emit()"
            data-testid="confirm-cancel"
          >
            {{ 'common.cancel' | transloco }}
          </button>
          <button
            type="button"
            class="rounded-full px-4 py-2 text-sm font-medium text-white shadow-card disabled:opacity-60"
            [class]="danger() ? 'bg-negative hover:opacity-90' : 'bg-ink hover:bg-ink-soft'"
            [disabled]="busy()"
            (click)="confirmed.emit()"
            data-testid="confirm-ok"
          >
            {{ confirmLabel() }}
          </button>
        </div>
      </div>
    </div>
  `,
})
export class ConfirmDialog {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly confirmLabel = input.required<string>();
  readonly danger = input(false);
  readonly busy = input(false);

  readonly confirmed = output<void>();
  readonly dismissed = output<void>();
}
