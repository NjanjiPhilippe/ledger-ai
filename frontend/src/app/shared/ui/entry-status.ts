import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { JournalEntryStatus } from '../../core/api/api-types';

const CLASSES: Record<JournalEntryStatus, string> = {
  DRAFT: 'bg-fog text-graphite',
  POSTED: 'bg-positive-soft text-positive',
  REVERSED: 'bg-negative-soft text-negative',
};

/** The status of a journal entry as a small translated pill, the same everywhere. */
@Component({
  selector: 'app-entry-status',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-block rounded-full px-2.5 py-0.5 text-xs font-medium"
      [class]="classes()"
      data-testid="entry-status"
    >
      {{ 'entryStatus.' + status() | transloco }}
    </span>
  `,
})
export class EntryStatus {
  readonly status = input.required<JournalEntryStatus>();

  protected classes(): string {
    return CLASSES[this.status()];
  }
}
