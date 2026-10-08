import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { JournalEntry, JournalEntryStatus } from '../../../core/api/api-types';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { EntryStatus } from '../../../shared/ui/entry-status';
import { AppIcon } from '../../../shared/ui/icon';

interface EntryRow {
  readonly id: string;
  readonly key: string;
  readonly description: string;
  readonly status: JournalEntryStatus;
  readonly date: string;
  readonly debited: readonly (string | null)[];
  readonly credited: readonly (string | null)[];
}

/** Recent journal entries described with account names (a null name is shown as "unknown account"). */
@Component({
  selector: 'app-recent-entries-list',
  imports: [RouterLink, TranslocoPipe, ShortDatePipe, AppIcon, EntryStatus],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (rows().length === 0) {
      <div
        class="flex flex-col items-center gap-2 rounded-xl border border-dashed border-silver p-8 text-center text-graphite"
        data-testid="entries-empty"
      >
        <app-icon name="book" class="size-7" />
        <p class="text-sm">{{ 'dashboard.entries.empty' | transloco }}</p>
      </div>
    } @else {
      <ul class="divide-y divide-mist">
        @for (row of rows(); track row.key) {
          <li data-testid="entry-row">
            <a
              [routerLink]="['/entries', row.id]"
              class="-mx-2 flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl px-2 py-3 hover:bg-fog focus-visible:outline-2 focus-visible:outline-mint"
            >
              <span
                class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-fog text-graphite"
              >
                <app-icon name="book" class="size-[18px]" />
              </span>
              <span class="min-w-0 flex-1 basis-56">
                <span class="block truncate font-medium">{{ row.description }}</span>
                <span class="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-xs text-graphite">
                  <span data-testid="entry-debited">{{ names(row.debited) }}</span>
                  <app-icon name="arrowUpRight" class="size-3" />
                  <span data-testid="entry-credited">{{ names(row.credited) }}</span>
                </span>
              </span>
              <span class="text-xs text-graphite">{{ row.date | shortDate }}</span>
              <app-entry-status [status]="row.status" />
            </a>
          </li>
        }
      </ul>
    }
  `,
})
export class RecentEntriesList {
  readonly entries = input.required<readonly JournalEntry[]>();
  readonly accountNames = input.required<ReadonlyMap<string, string>>();
  readonly unknownAccount = input.required<string>();

  protected readonly rows = computed<EntryRow[]>(() => {
    const names = this.accountNames();
    const nameOf = (id: string) => names.get(id) ?? null;
    return this.entries().map((entry) => ({
      id: entry.id,
      key: entry.id,
      description: entry.description,
      status: entry.status,
      date: entry.postedAt ?? entry.createdAt,
      debited: unique(
        entry.lines.filter((l) => l.entryType === 'DEBIT').map((l) => nameOf(l.accountId)),
      ),
      credited: unique(
        entry.lines.filter((l) => l.entryType === 'CREDIT').map((l) => nameOf(l.accountId)),
      ),
    }));
  });

  protected names(list: readonly (string | null)[]): string {
    return list.map((n) => n ?? this.unknownAccount()).join(', ');
  }
}

function unique<T>(values: readonly T[]): T[] {
  return Array.from(new Set(values));
}
