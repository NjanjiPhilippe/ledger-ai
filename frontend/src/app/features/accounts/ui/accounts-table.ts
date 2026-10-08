import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcon, IconName } from '../../../shared/ui/icon';
import { AccountResponse, AccountType } from '../../../core/api/api-types';

const ICONS: Record<AccountType, IconName> = {
  ASSET: 'wallet',
  LIABILITY: 'receipt',
  EQUITY: 'pie',
  REVENUE: 'trendingUp',
  EXPENSE: 'trendingDown',
};

/** Presentation only: receives accounts, renders them, knows nothing about the API or the state. */
@Component({
  selector: 'app-accounts-table',
  imports: [TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `
    <div class="overflow-x-auto rounded-2xl border border-mist bg-white shadow-card">
      <table class="min-w-full divide-y divide-mist text-sm">
        <thead class="text-left text-xs uppercase tracking-wider text-graphite">
          <tr>
            <th scope="col" class="px-4 py-2 font-medium">
              {{ 'accounts.columns.name' | transloco }}
            </th>
            <th scope="col" class="px-4 py-2 font-medium">
              {{ 'accounts.columns.type' | transloco }}
            </th>
            <th scope="col" class="px-4 py-2 font-medium">
              {{ 'accounts.columns.currency' | transloco }}
            </th>
            <th scope="col" class="px-4 py-2 font-medium">
              {{ 'accounts.columns.status' | transloco }}
            </th>
            @if (canEdit()) {
              <th scope="col" class="px-4 py-2 text-right font-medium">
                <span class="sr-only">{{ 'accounts.columns.actions' | transloco }}</span>
              </th>
            }
          </tr>
        </thead>
        <tbody class="divide-y divide-mist">
          @for (account of accounts(); track account.id) {
            <tr data-testid="account-row">
              <td class="px-4 py-2.5">
                <div class="flex items-center gap-3">
                  <span
                    class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-fog text-graphite"
                  >
                    <app-icon [name]="iconOf(account.type)" class="size-4" />
                  </span>
                  <span class="font-medium">{{ account.name }}</span>
                </div>
              </td>
              <td class="px-4 py-2.5">{{ 'accountType.' + account.type | transloco }}</td>
              <td class="px-4 py-2.5 tabular-nums">{{ account.currencyCode }}</td>
              <td class="px-4 py-2.5">
                <span
                  class="rounded-full px-2.5 py-0.5 text-xs font-medium"
                  [class]="
                    account.active ? 'bg-positive-soft text-positive' : 'bg-fog text-graphite'
                  "
                >
                  {{
                    (account.active ? 'accounts.status.active' : 'accounts.status.inactive')
                      | transloco
                  }}
                </span>
              </td>
              @if (canEdit()) {
                <td class="px-4 py-2.5 text-right">
                  <button
                    type="button"
                    class="inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-medium text-graphite hover:bg-fog hover:text-ink"
                    [attr.aria-label]="'accounts.edit' | transloco: { name: account.name }"
                    (click)="edit.emit(account)"
                    data-testid="edit-account"
                  >
                    <app-icon name="pencil" class="size-3.5" />
                    {{ 'accounts.editShort' | transloco }}
                  </button>
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AccountsTable {
  readonly accounts = input.required<readonly AccountResponse[]>();
  readonly canEdit = input(false);
  readonly edit = output<AccountResponse>();

  protected iconOf(type: AccountType): IconName {
    return ICONS[type];
  }
}
