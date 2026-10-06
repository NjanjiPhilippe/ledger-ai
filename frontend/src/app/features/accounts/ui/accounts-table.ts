import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountResponse } from '../../../core/api/api-types';

/** Presentation only: receives accounts, renders them, knows nothing about the API or the state. */
@Component({
  selector: 'app-accounts-table',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="overflow-x-auto rounded-lg border border-slate-200 bg-white shadow-sm">
      <table class="min-w-full divide-y divide-slate-200 text-sm">
        <thead class="bg-slate-50 text-left text-slate-600">
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
          </tr>
        </thead>
        <tbody class="divide-y divide-slate-100">
          @for (account of accounts(); track account.id) {
            <tr data-testid="account-row">
              <td class="px-4 py-2 font-medium">{{ account.name }}</td>
              <td class="px-4 py-2">{{ 'accountType.' + account.type | transloco }}</td>
              <td class="px-4 py-2 font-mono">{{ account.currencyCode }}</td>
              <td class="px-4 py-2">
                <span
                  class="rounded px-2 py-0.5 text-xs"
                  [class]="
                    account.active ? 'bg-green-100 text-green-800' : 'bg-slate-200 text-slate-700'
                  "
                >
                  {{
                    (account.active ? 'accounts.status.active' : 'accounts.status.inactive')
                      | transloco
                  }}
                </span>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AccountsTable {
  readonly accounts = input.required<readonly AccountResponse[]>();
}
