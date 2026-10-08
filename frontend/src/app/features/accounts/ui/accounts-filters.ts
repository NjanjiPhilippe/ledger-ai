import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  input,
  output,
} from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountFilters, AccountType } from '../../../core/api/api-types';
import { AppIcon } from '../../../shared/ui/icon';
import { ACCOUNT_TYPES } from './account-form-dialog';

/** Search box and two selects. Emits the change, never filters by itself. */
@Component({
  selector: 'app-accounts-filters',
  imports: [TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="flex flex-wrap items-center gap-3">
      <div class="relative min-w-56 flex-1 sm:max-w-sm">
        <app-icon
          name="search"
          class="pointer-events-none absolute left-3.5 top-1/2 size-4 -translate-y-1/2 text-steel"
        />
        <input
          type="search"
          class="w-full rounded-full border border-silver bg-white py-2.5 pl-10 pr-4 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
          [value]="filters().name ?? ''"
          [attr.aria-label]="'accounts.filters.search' | transloco"
          [placeholder]="'accounts.filters.search' | transloco"
          (input)="searchInput($any($event.target).value)"
          data-testid="search"
        />
      </div>

      <select
        class="rounded-full border border-silver bg-white px-4 py-2.5 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
        [attr.aria-label]="'accounts.filters.type' | transloco"
        (change)="typeChange($any($event.target).value)"
        data-testid="type-filter"
      >
        <option value="" [selected]="!filters().type">
          {{ 'accounts.filters.allTypes' | transloco }}
        </option>
        @for (type of types; track type) {
          <option [value]="type" [selected]="filters().type === type">
            {{ 'accountType.' + type | transloco }}
          </option>
        }
      </select>

      <select
        class="rounded-full border border-silver bg-white px-4 py-2.5 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
        [attr.aria-label]="'accounts.filters.status' | transloco"
        (change)="statusChange($any($event.target).value)"
        data-testid="status-filter"
      >
        <option value="" [selected]="filters().active === undefined">
          {{ 'accounts.filters.allStatuses' | transloco }}
        </option>
        <option value="active" [selected]="filters().active === true">
          {{ 'accounts.status.active' | transloco }}
        </option>
        <option value="inactive" [selected]="filters().active === false">
          {{ 'accounts.status.inactive' | transloco }}
        </option>
      </select>

      @if (hasFilters()) {
        <button
          type="button"
          class="rounded-full px-3 py-2 text-sm font-medium text-graphite hover:bg-white hover:text-ink"
          (click)="clear.emit()"
          data-testid="clear-filters"
        >
          {{ 'accounts.filters.clear' | transloco }}
        </button>
      }
    </div>
  `,
})
export class AccountsFilters {
  readonly filters = input.required<AccountFilters>();
  readonly hasFilters = input(false);
  readonly filterChange = output<Partial<AccountFilters>>();
  readonly clear = output<void>();

  protected readonly types = ACCOUNT_TYPES;
  private timer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    // A search typed just before leaving the screen must not fire on a screen that is gone.
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  /** Waits for a pause in typing so that every keystroke does not trigger a request. */
  protected searchInput(value: string): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => this.filterChange.emit({ name: value }), 300);
  }

  protected typeChange(value: string): void {
    this.filterChange.emit({ type: (value || undefined) as AccountType | undefined });
  }

  protected statusChange(value: string): void {
    this.filterChange.emit({ active: value === '' ? undefined : value === 'active' });
  }
}
