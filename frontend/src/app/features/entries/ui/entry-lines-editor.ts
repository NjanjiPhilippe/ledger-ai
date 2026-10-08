import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountResponse, EntryType } from '../../../core/api/api-types';
import { AppIcon } from '../../../shared/ui/icon';

/** What the editor needs to know about a line: the store's LineDraft, without importing the store. */
export interface EditableLine {
  readonly id: number;
  readonly accountId: string;
  readonly side: EntryType;
  readonly amount: string;
}

export interface LineIssues {
  readonly account: boolean;
  readonly amount: boolean;
}

export interface LineChange {
  readonly id: number;
  readonly patch: Partial<Pick<EditableLine, 'accountId' | 'side' | 'amount'>>;
}

/** The lines of an entry as editable rows. Presentation only: every edit is emitted, nothing is stored here. */
@Component({
  selector: 'app-entry-lines-editor',
  imports: [TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <ol class="space-y-3">
      @for (line of lines(); track line.id; let index = $index) {
        <li
          class="grid grid-cols-1 items-start gap-3 rounded-2xl bg-fog p-3 sm:grid-cols-[minmax(0,1fr)_auto_10rem_auto]"
          data-testid="entry-line"
        >
          <div class="space-y-1">
            <label [for]="'account-' + line.id" class="sr-only">
              {{ 'entry.account' | transloco }} ({{ 'entry.line' | transloco: { n: index + 1 } }})
            </label>
            <select
              [id]="'account-' + line.id"
              class="w-full rounded-lg border bg-white px-3 py-2.5 focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
              [class]="
                showIssues() && issues().get(line.id)?.account ? 'border-negative' : 'border-silver'
              "
              (change)="
                lineChange.emit({ id: line.id, patch: { accountId: $any($event.target).value } })
              "
              data-testid="line-account"
            >
              <option value="" [selected]="line.accountId === ''">
                {{ 'entry.chooseAccount' | transloco }}
              </option>
              @for (account of accounts(); track account.id) {
                <option [value]="account.id" [selected]="account.id === line.accountId">
                  {{ account.name }}
                </option>
              }
            </select>
            @if (showIssues() && issues().get(line.id)?.account) {
              <p class="text-xs text-negative" data-testid="line-account-error">
                {{ 'entry.errors.account' | transloco }}
              </p>
            }
          </div>

          <div
            class="inline-flex overflow-hidden rounded-lg border border-silver bg-white text-sm font-medium"
            role="group"
            [attr.aria-label]="'entry.side' | transloco"
          >
            @for (side of sides; track side) {
              <button
                type="button"
                class="px-3 py-2.5"
                [class]="line.side === side ? 'bg-ink text-white' : 'text-graphite hover:bg-fog'"
                [attr.aria-pressed]="line.side === side"
                (click)="lineChange.emit({ id: line.id, patch: { side } })"
                [attr.data-testid]="'side-' + side.toLowerCase()"
              >
                {{ (side === 'DEBIT' ? 'entry.debit' : 'entry.credit') | transloco }}
              </button>
            }
          </div>

          <div class="space-y-1">
            <label [for]="'amount-' + line.id" class="sr-only">
              {{ 'entry.amount' | transloco }} ({{ 'entry.line' | transloco: { n: index + 1 } }})
            </label>
            <input
              [id]="'amount-' + line.id"
              type="text"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="w-full rounded-lg border bg-white px-3 py-2.5 text-right tabular-nums focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
              [class]="
                showIssues() && issues().get(line.id)?.amount ? 'border-negative' : 'border-silver'
              "
              [value]="line.amount"
              (input)="
                lineChange.emit({ id: line.id, patch: { amount: $any($event.target).value } })
              "
              data-testid="line-amount"
            />
            @if (showIssues() && issues().get(line.id)?.amount) {
              <p class="text-xs text-negative" data-testid="line-amount-error">
                {{ 'entry.errors.amount' | transloco }}
              </p>
            }
          </div>

          <button
            type="button"
            class="justify-self-end rounded-lg p-2.5 text-graphite hover:bg-white hover:text-negative disabled:opacity-40"
            [disabled]="lines().length <= 2"
            [attr.aria-label]="'entry.removeLine' | transloco: { n: index + 1 }"
            (click)="remove.emit(line.id)"
            data-testid="remove-line"
          >
            <app-icon name="close" class="size-4" />
          </button>
        </li>
      }
    </ol>

    <div class="mt-3 flex flex-wrap gap-2">
      <button
        type="button"
        class="inline-flex items-center gap-2 rounded-full border border-silver bg-white px-4 py-2 text-sm font-medium hover:bg-fog"
        (click)="add.emit('DEBIT')"
        data-testid="add-debit"
      >
        <app-icon name="plus" class="size-4" /> {{ 'entry.addDebit' | transloco }}
      </button>
      <button
        type="button"
        class="inline-flex items-center gap-2 rounded-full border border-silver bg-white px-4 py-2 text-sm font-medium hover:bg-fog"
        (click)="add.emit('CREDIT')"
        data-testid="add-credit"
      >
        <app-icon name="plus" class="size-4" /> {{ 'entry.addCredit' | transloco }}
      </button>
    </div>
  `,
})
export class EntryLinesEditor {
  readonly lines = input.required<readonly EditableLine[]>();
  readonly accounts = input.required<readonly AccountResponse[]>();
  readonly issues = input.required<ReadonlyMap<number, LineIssues>>();
  readonly showIssues = input(false);

  readonly lineChange = output<LineChange>();
  readonly add = output<EntryType>();
  readonly remove = output<number>();

  protected readonly sides: readonly EntryType[] = ['DEBIT', 'CREDIT'];
}
