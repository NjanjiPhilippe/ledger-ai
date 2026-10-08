import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountType, BalanceGroup } from '../../../core/api/api-types';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { ACCOUNT_TYPE_ICONS } from '../../../shared/ui/account-type-icons';
import { AppIcon, IconName } from '../../../shared/ui/icon';

/** The balance as a table grouped by account type, with a closing total row. Presentation only. */
@Component({
  selector: 'app-balance-table',
  imports: [TranslocoPipe, MoneyPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="overflow-x-auto">
      <table class="min-w-full text-sm" data-testid="balance-table">
        <thead class="text-left text-xs uppercase tracking-wider text-graphite">
          <tr class="border-b border-mist">
            <th scope="col" class="px-5 py-3 font-medium">
              {{ 'dashboard.trialBalance.columns.account' | transloco }}
            </th>
            <th scope="col" class="px-5 py-3 text-right font-medium">
              {{ 'dashboard.trialBalance.columns.debit' | transloco }}
            </th>
            <th scope="col" class="px-5 py-3 text-right font-medium">
              {{ 'dashboard.trialBalance.columns.credit' | transloco }}
            </th>
            <th scope="col" class="px-5 py-3 text-right font-medium">
              {{ 'dashboard.trialBalance.columns.balance' | transloco }}
            </th>
          </tr>
        </thead>
        @for (group of groups(); track group.type) {
          <tbody class="divide-y divide-mist" data-testid="balance-group">
            <tr class="bg-fog">
              <th scope="colgroup" colspan="4" class="px-5 py-2.5 text-left">
                <span class="inline-flex items-center gap-2 font-bold">
                  <app-icon [name]="iconOf(group.type)" class="size-4 text-graphite" />
                  {{ 'accountType.' + group.type | transloco }}
                  <span
                    class="rounded-full bg-white px-2 py-0.5 text-xs font-medium text-graphite"
                    >{{ group.lines.length }}</span
                  >
                </span>
              </th>
            </tr>
            @for (line of group.lines; track line.accountId) {
              <tr class="hover:bg-fog/60" data-testid="balance-row">
                <td class="px-5 py-2.5 font-medium">{{ line.accountName }}</td>
                <td class="px-5 py-2.5 text-right tabular-nums">
                  {{ line.totalDebits | money: currency() }}
                </td>
                <td class="px-5 py-2.5 text-right tabular-nums">
                  {{ line.totalCredits | money: currency() }}
                </td>
                <td
                  class="px-5 py-2.5 text-right font-medium tabular-nums"
                  [class.text-negative]="line.balance.startsWith('-')"
                >
                  {{ line.balance | money: currency() }}
                </td>
              </tr>
            }
            <tr class="text-graphite" data-testid="group-total">
              <th
                scope="row"
                class="px-5 py-2 text-left text-xs font-medium uppercase tracking-wider"
              >
                {{ 'balance.subtotal' | transloco }}
              </th>
              <td class="px-5 py-2 text-right text-xs tabular-nums">
                {{ group.totalDebits | money: currency() }}
              </td>
              <td class="px-5 py-2 text-right text-xs tabular-nums">
                {{ group.totalCredits | money: currency() }}
              </td>
              <td></td>
            </tr>
          </tbody>
        }
        <tfoot>
          <tr class="bg-ink text-white" data-testid="balance-total">
            <th scope="row" class="rounded-bl-2xl px-5 py-4 text-left font-bold">
              <span class="inline-flex items-center gap-2">
                <app-icon
                  [name]="balanced() ? 'checkCircle' : 'alertTriangle'"
                  class="size-[18px] text-mint"
                />
                {{ 'balance.grandTotal' | transloco }}
              </span>
            </th>
            <td
              class="px-5 py-4 text-right font-bold tabular-nums text-mint"
              data-testid="grand-debits"
            >
              {{ totalDebits() | money: currency() }}
            </td>
            <td
              class="px-5 py-4 text-right font-bold tabular-nums text-mint"
              data-testid="grand-credits"
            >
              {{ totalCredits() | money: currency() }}
            </td>
            <td class="rounded-br-2xl"></td>
          </tr>
        </tfoot>
      </table>
    </div>
  `,
})
export class BalanceTable {
  readonly groups = input.required<readonly BalanceGroup[]>();
  readonly currency = input.required<string>();
  readonly totalDebits = input.required<string>();
  readonly totalCredits = input.required<string>();
  readonly balanced = input.required<boolean>();

  protected iconOf(type: AccountType): IconName {
    return ACCOUNT_TYPE_ICONS[type];
  }
}
