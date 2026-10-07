import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LoadingService } from './loading.service';

/** A thin bar across the top of the window while the API is busy. It only fades in after a short delay (see styles.css). */
@Component({
  selector: 'app-loading-bar',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading.busy()) {
      <div
        class="loading-bar pointer-events-none fixed inset-x-0 top-0 z-50 h-1 overflow-hidden bg-slate-200"
        role="progressbar"
        [attr.aria-label]="'common.loading' | transloco"
        data-testid="loading-bar"
      >
        <div class="loading-bar__indicator h-full w-1/3 bg-slate-800"></div>
      </div>
    }
  `,
})
export class LoadingBar {
  protected readonly loading = inject(LoadingService);
}
