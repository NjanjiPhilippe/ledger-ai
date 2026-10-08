import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountType } from '../../../core/api/api-types';
import { AppIcon } from '../../../shared/ui/icon';
import { ACCOUNT_TYPE_ICONS } from './account-type-icon';

const ORDER: readonly AccountType[] = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'];

/** How the chart of accounts is spread over the five accounting families. Presentation only. */
@Component({
  selector: 'app-account-types-list',
  imports: [TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="space-y-4">
      @for (row of rows(); track row.type) {
        <li data-testid="account-type-row">
          <div class="flex items-center gap-3">
            <span
              class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-fog text-graphite"
            >
              <app-icon [name]="row.icon" class="size-4" />
            </span>
            <span class="flex-1 text-sm font-medium">{{
              'accountType.' + row.type | transloco
            }}</span>
            <span class="text-sm font-bold tabular-nums" data-testid="account-type-count">{{
              row.count
            }}</span>
          </div>
          <div class="mt-2 h-1.5 overflow-hidden rounded-full bg-fog" aria-hidden="true">
            <!-- Width only: this ratio is never shown as a figure. -->
            <div class="h-full rounded-full bg-ink" [style.width.%]="row.share"></div>
          </div>
        </li>
      }
    </ul>
  `,
})
export class AccountTypesList {
  readonly counts = input.required<ReadonlyMap<AccountType, number>>();

  protected readonly rows = computed(() => {
    const counts = this.counts();
    const largest = Math.max(1, ...ORDER.map((t) => counts.get(t) ?? 0));
    return ORDER.map((type) => {
      const count = counts.get(type) ?? 0;
      return { type, count, icon: ACCOUNT_TYPE_ICONS[type], share: (count / largest) * 100 };
    });
  });
}
