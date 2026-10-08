import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { EntryFilters, JournalEntryStatus } from '../../../core/api/api-types';

const STATUSES: readonly JournalEntryStatus[] = ['DRAFT', 'POSTED', 'REVERSED'];

/** Status and creation dates. Emits the change; the dates are whole days, in UTC. */
@Component({
  selector: 'app-entries-filters',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="flex flex-wrap items-end gap-3">
      <div class="space-y-1">
        <label for="entries-status" class="block text-xs font-medium text-graphite">
          {{ 'entries.filters.status' | transloco }}
        </label>
        <select
          id="entries-status"
          class="rounded-full border border-silver bg-white px-4 py-2.5 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
          (change)="statusChange($any($event.target).value)"
          data-testid="status-filter"
        >
          <option value="" [selected]="!filters().status">
            {{ 'entries.filters.allStatuses' | transloco }}
          </option>
          @for (status of statuses; track status) {
            <option [value]="status" [selected]="filters().status === status">
              {{ 'entryStatus.' + status | transloco }}
            </option>
          }
        </select>
      </div>
      <div class="space-y-1">
        <label for="entries-from" class="block text-xs font-medium text-graphite">
          {{ 'entries.filters.from' | transloco }}
        </label>
        <input
          id="entries-from"
          type="date"
          class="rounded-full border border-silver bg-white px-4 py-2 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
          [value]="day(filters().createdFrom)"
          (change)="fromChange($any($event.target).value)"
          data-testid="from-filter"
        />
      </div>
      <div class="space-y-1">
        <label for="entries-to" class="block text-xs font-medium text-graphite">
          {{ 'entries.filters.to' | transloco }}
        </label>
        <input
          id="entries-to"
          type="date"
          class="rounded-full border border-silver bg-white px-4 py-2 text-sm focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
          [value]="day(filters().createdTo)"
          (change)="toChange($any($event.target).value)"
          data-testid="to-filter"
        />
      </div>
      @if (hasFilters()) {
        <button
          type="button"
          class="rounded-full px-3 py-2 text-sm font-medium text-graphite hover:bg-white hover:text-ink"
          (click)="clear.emit()"
          data-testid="clear-filters"
        >
          {{ 'entries.filters.clear' | transloco }}
        </button>
      }
    </div>
  `,
})
export class EntriesFilters {
  readonly filters = input.required<EntryFilters>();
  readonly hasFilters = input(false);
  readonly filterChange = output<Partial<EntryFilters>>();
  readonly clear = output<void>();

  protected readonly statuses = STATUSES;

  /** "2026-10-08T00:00:00Z" back to "2026-10-08", what a date input shows. */
  protected day(instant: string | undefined): string {
    return instant ? instant.slice(0, 10) : '';
  }

  protected statusChange(value: string): void {
    this.filterChange.emit({ status: (value || undefined) as JournalEntryStatus | undefined });
  }

  protected fromChange(date: string): void {
    this.filterChange.emit({ createdFrom: date ? `${date}T00:00:00Z` : undefined });
  }

  protected toChange(date: string): void {
    this.filterChange.emit({ createdTo: date ? `${date}T23:59:59Z` : undefined });
  }
}
