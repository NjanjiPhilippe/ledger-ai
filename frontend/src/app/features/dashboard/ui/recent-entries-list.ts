import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { JournalEntry, JournalEntryStatus } from '../../../core/api/api-types';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { AppIcon } from '../../../shared/ui/icon';

const STATUS_CLASSES: Record<JournalEntryStatus, string> = {
  DRAFT: 'bg-fog text-graphite',
  POSTED: 'bg-positive-soft text-positive',
  REVERSED: 'bg-negative-soft text-negative',
};

interface EntryRow {
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
  imports: [TranslocoPipe, ShortDatePipe, AppIcon],
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
          <li class="flex flex-wrap items-center gap-x-4 gap-y-2 py-3" data-testid="entry-row">
            <span
              class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-fog text-graphite"
            >
              <app-icon name="book" class="size-[18px]" />
            </span>
            <div class="min-w-0 flex-1 basis-56">
              <p class="truncate font-medium">{{ row.description }}</p>
              <p class="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-xs text-graphite">
                <span data-testid="entry-debited">{{ names(row.debited) }}</span>
                <app-icon name="arrowUpRight" class="size-3" />
                <span data-testid="entry-credited">{{ names(row.credited) }}</span>
              </p>
            </div>
            <span class="text-xs text-graphite">{{ row.date | shortDate }}</span>
            <span
              class="rounded-full px-2.5 py-0.5 text-xs font-medium"
              [class]="statusClasses(row.status)"
              data-testid="entry-status"
            >
              {{ 'entryStatus.' + row.status | transloco }}
            </span>
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

  protected statusClasses(status: JournalEntryStatus): string {
    return STATUS_CLASSES[status];
  }

  protected names(list: readonly (string | null)[]): string {
    return list.map((n) => n ?? this.unknownAccount()).join(', ');
  }
}

function unique<T>(values: readonly T[]): T[] {
  return Array.from(new Set(values));
}
