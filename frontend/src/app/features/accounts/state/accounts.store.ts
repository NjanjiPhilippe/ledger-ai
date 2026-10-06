import { computed, inject, Injectable, signal } from '@angular/core';
import { PagedAccounts } from '../../../core/api/api-types';
import { AccountsApi } from '../data-access/accounts.api';

export type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

const PAGE_SIZE = 20;

/** State of the accounts screen, provided by the route so that it starts fresh each time the screen opens. */
@Injectable()
export class AccountsStore {
  private readonly api = inject(AccountsApi);

  private readonly pageState = signal<PagedAccounts | null>(null);
  private readonly statusState = signal<LoadStatus>('idle');
  private requested = 0;

  readonly status = this.statusState.asReadonly();
  readonly accounts = computed(() => this.pageState()?.content ?? []);
  /** 0-based, as in the API. */
  readonly pageIndex = computed(() => this.pageState()?.page ?? this.requested);
  readonly totalPages = computed(() => this.pageState()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.pageState()?.totalElements ?? 0);
  readonly hasPrevious = computed(() => this.pageIndex() > 0);
  readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());

  load(page = this.requested): void {
    this.requested = page;
    this.statusState.set('loading');
    this.api.list(page, PAGE_SIZE).subscribe({
      next: (result) => {
        // A slower, older response must not overwrite the page the user asked for last.
        if (page !== this.requested) {
          return;
        }
        this.pageState.set(result);
        this.statusState.set('loaded');
      },
      // The failure is already reported by the error interceptor (toast): the screen only offers a retry.
      error: () => {
        if (page === this.requested) {
          this.statusState.set('error');
        }
      },
    });
  }

  next(): void {
    if (this.hasNext()) {
      this.load(this.pageIndex() + 1);
    }
  }

  previous(): void {
    if (this.hasPrevious()) {
      this.load(this.pageIndex() - 1);
    }
  }
}
