import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { AccountsStore } from '../state/accounts.store';
import { AccountsTable } from '../ui/accounts-table';

@Component({
  selector: 'app-accounts-page',
  imports: [AccountsTable, TranslocoPipe],
  providers: [AccountsStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-baseline justify-between">
      <h1 class="text-2xl font-semibold">{{ 'accounts.title' | transloco }}</h1>
      @if (store.status() === 'loaded') {
        <p class="text-sm text-slate-600" data-testid="total">
          {{ 'accounts.total' | transloco: { count: store.totalElements() } }}
        </p>
      }
    </div>

    <div class="mt-4">
      @switch (store.status()) {
        @case ('error') {
          <div
            class="rounded-lg border border-red-200 bg-red-50 p-4 text-red-900"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'accounts.loadFailed' | transloco }}</p>
            <button
              type="button"
              class="mt-2 rounded bg-red-700 px-3 py-1 text-sm text-white hover:bg-red-800"
              (click)="store.load()"
            >
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @case ('loaded') {
          @if (store.accounts().length === 0) {
            <p
              class="rounded-lg border border-dashed border-slate-300 p-6 text-center text-slate-600"
              data-testid="empty"
            >
              {{ 'accounts.empty' | transloco }}
            </p>
          } @else {
            <app-accounts-table [accounts]="store.accounts()" />
            <nav class="mt-3 flex items-center justify-between text-sm" aria-label="pagination">
              <button
                type="button"
                class="rounded border border-slate-300 px-3 py-1 disabled:opacity-50"
                [disabled]="!store.hasPrevious()"
                (click)="store.previous()"
              >
                {{ 'common.previous' | transloco }}
              </button>
              <span data-testid="page-of">
                {{
                  'common.pageOf'
                    | transloco: { page: store.pageIndex() + 1, total: store.totalPages() }
                }}
              </span>
              <button
                type="button"
                class="rounded border border-slate-300 px-3 py-1 disabled:opacity-50"
                [disabled]="!store.hasNext()"
                (click)="store.next()"
              >
                {{ 'common.next' | transloco }}
              </button>
            </nav>
          }
        }
        @default {
          <p class="text-slate-600" role="status" data-testid="loading">
            {{ 'common.loading' | transloco }}
          </p>
        }
      }
    </div>
  `,
})
export class AccountsPage implements OnInit {
  protected readonly store = inject(AccountsStore);

  ngOnInit(): void {
    this.store.load(0);
  }
}
