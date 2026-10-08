import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountType, TrialBalance } from '../../../core/api/api-types';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { AppIcon } from '../../../shared/ui/icon';
import { ACCOUNT_TYPE_ICONS } from './account-type-icon';

/** Presentation only: the lines of the trial balance, with account names and a closing total row. */
@Component({
  selector: 'app-trial-balance-table',
  imports: [TranslocoPipe, MoneyPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (report().lines.length === 0) {
      <div
        class="flex flex-col items-center gap-2 rounded-xl border border-dashed border-silver p-8 text-center text-graphite"
        data-testid="trial-balance-empty"
      >
        <app-icon name="inbox" class="size-7" />
        <p class="text-sm">{{ 'dashboard.trialBalance.empty' | transloco }}</p>
      </div>
    } @else {
      <div class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead class="text-left text-xs uppercase tracking-wider text-graphite">
            <tr>
              <th scope="col" class="py-2 pr-4 font-medium">
                {{ 'dashboard.trialBalance.columns.account' | transloco }}
              </th>
              <th scope="col" class="px-4 py-2 text-right font-medium">
                {{ 'dashboard.trialBalance.columns.debit' | transloco }}
              </th>
              <th scope="col" class="px-4 py-2 text-right font-medium">
                {{ 'dashboard.trialBalance.columns.credit' | transloco }}
              </th>
              <th scope="col" class="py-2 pl-4 text-right font-medium">
                {{ 'dashboard.trialBalance.columns.balance' | transloco }}
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-mist">
            @for (line of report().lines; track line.accountId) {
              <tr data-testid="trial-balance-row">
                <td class="py-2.5 pr-4">
                  <div class="flex items-center gap-3">
                    <span
                      class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-fog text-graphite"
                    >
                      <app-icon [name]="iconOf(line.accountType)" class="size-4" />
                    </span>
                    <div class="min-w-0">
                      <p class="truncate font-medium">{{ line.accountName }}</p>
                      <p class="text-xs text-graphite">
                        {{ 'accountType.' + line.accountType | transloco }}
                      </p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-2.5 text-right tabular-nums">
                  {{ line.totalDebits | money: report().currencyCode }}
                </td>
                <td class="px-4 py-2.5 text-right tabular-nums">
                  {{ line.totalCredits | money: report().currencyCode }}
                </td>
                <td
                  class="py-2.5 pl-4 text-right font-medium tabular-nums"
                  [class.text-negative]="line.balance.startsWith('-')"
                >
                  {{ line.balance | money: report().currencyCode }}
                </td>
              </tr>
            }
          </tbody>
          <tfoot class="border-t-2 border-ink font-bold">
            <tr data-testid="trial-balance-total">
              <th scope="row" class="py-3 pr-4 text-left">
                {{ 'dashboard.trialBalance.total' | transloco }}
              </th>
              <td class="px-4 py-3 text-right tabular-nums">
                {{ report().totalDebits | money: report().currencyCode }}
              </td>
              <td class="px-4 py-3 text-right tabular-nums">
                {{ report().totalCredits | money: report().currencyCode }}
              </td>
              <td></td>
            </tr>
          </tfoot>
        </table>
      </div>
    }
  `,
})
export class TrialBalanceTable {
  readonly report = input.required<TrialBalance>();

  protected iconOf(type: string) {
    return ACCOUNT_TYPE_ICONS[type as AccountType] ?? 'wallet';
  }
}
