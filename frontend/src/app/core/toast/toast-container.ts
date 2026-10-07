import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { Toast, ToastLevel, ToastService } from './toast.service';

// Full class names (no string building): Tailwind only generates the classes it can read in the source.
const LEVEL_CLASSES: Record<ToastLevel, string> = {
  success: 'border-green-300 bg-green-50 text-green-900',
  info: 'border-sky-300 bg-sky-50 text-sky-900',
  warning: 'border-amber-300 bg-amber-50 text-amber-900',
  error: 'border-red-300 bg-red-50 text-red-900',
};

@Component({
  selector: 'app-toast-container',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="pointer-events-none fixed right-4 top-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2"
    >
      @for (toast of toasts.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 rounded-md border p-3 shadow-md"
          [class]="classesOf(toast)"
          [attr.role]="roleOf(toast)"
          data-testid="toast"
        >
          <div class="min-w-0 flex-1">
            <p class="font-medium">{{ toast.title }}</p>
            @if (toast.detail) {
              <p class="mt-0.5 break-words text-sm opacity-90">{{ toast.detail }}</p>
            }
          </div>
          <button
            type="button"
            class="rounded px-1 text-lg leading-none opacity-60 hover:opacity-100 focus:outline-2 focus:outline-offset-1"
            [attr.aria-label]="'toast.dismiss' | transloco"
            (click)="toasts.dismiss(toast.id)"
          >
            ×
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastContainer {
  protected readonly toasts = inject(ToastService);

  protected classesOf(toast: Toast): string {
    return LEVEL_CLASSES[toast.level];
  }

  /** Problems interrupt a screen reader (alert); confirmations wait their turn (status). */
  protected roleOf(toast: Toast): 'alert' | 'status' {
    return toast.level === 'error' || toast.level === 'warning' ? 'alert' : 'status';
  }
}
