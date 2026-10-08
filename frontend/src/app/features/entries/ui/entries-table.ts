import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { JournalEntry } from '../../../core/api/api-types';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { AppIcon } from '../../../shared/ui/icon';
import { EntryStatus } from '../../../shared/ui/entry-status';

interface Row {
  readonly entry: JournalEntry;
  readonly debited: string;
  readonly credited: string;
}

/** Journal entries as rows: description, the accounts debited and credited by name, date and status. */
@Component({
  selector: 'app-entries-table',
  imports: [RouterLink, TranslocoPipe, ShortDatePipe, AppIcon, EntryStatus],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <ul
      class="divide-y divide-mist overflow-hidden rounded-2xl border border-mist bg-white shadow-card"
    >
      @for (row of rows(); track row.entry.id) {
        <li data-testid="entry-row">
          <a
            [routerLink]="['/entries', row.entry.id]"
            class="flex flex-wrap items-center gap-x-4 gap-y-2 px-5 py-3.5 hover:bg-fog focus-visible:outline-2 focus-visible:outline-mint"
          >
            <span
              class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-fog text-graphite"
            >
              <app-icon name="book" class="size-[18px]" />
            </span>
            <span class="min-w-0 flex-1 basis-56">
              <span class="block truncate font-medium" data-testid="entry-description">{{
                row.entry.description
              }}</span>
              <span class="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-xs text-graphite">
                <span data-testid="entry-debited">{{ row.debited }}</span>
                <app-icon name="arrowUpRight" class="size-3" />
                <span data-testid="entry-credited">{{ row.credited }}</span>
              </span>
            </span>
            <span class="text-xs text-graphite">{{
              row.entry.postedAt ?? row.entry.createdAt | shortDate
            }}</span>
            <app-entry-status [status]="row.entry.status" />
          </a>
        </li>
      }
    </ul>
  `,
})
export class EntriesTable {
  readonly entries = input.required<readonly JournalEntry[]>();
  readonly accountNames = input.required<ReadonlyMap<string, string>>();
  readonly unknownAccount = input.required<string>();

  protected readonly rows = computed<Row[]>(() => {
    const names = this.accountNames();
    const side = (entry: JournalEntry, type: 'DEBIT' | 'CREDIT') =>
      Array.from(
        new Set(
          entry.lines
            .filter((l) => l.entryType === type)
            .map((l) => names.get(l.accountId) ?? this.unknownAccount()),
        ),
      ).join(', ');
    return this.entries().map((entry) => ({
      entry,
      debited: side(entry, 'DEBIT'),
      credited: side(entry, 'CREDIT'),
    }));
  });
}
