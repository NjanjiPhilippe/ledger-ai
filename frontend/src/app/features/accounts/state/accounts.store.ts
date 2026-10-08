import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  AccountFilters,
  AccountResponse,
  CreateAccountRequest,
  PagedAccounts,
  UpdateAccountRequest,
} from '../../../core/api/api-types';
import { AccountsApi } from '../data-access/accounts.api';

export type LoadStatus = 'idle' | 'loading' | 'loaded' | 'error';

const PAGE_SIZE = 20;

/** State of the accounts screen, provided by the route so that it starts fresh each time the screen opens. */
@Injectable()
export class AccountsStore {
  private readonly api = inject(AccountsApi);

  private readonly pageState = signal<PagedAccounts | null>(null);
  private readonly statusState = signal<LoadStatus>('idle');
  private readonly filtersState = signal<AccountFilters>({});
  private requested = 0;

  readonly status = this.statusState.asReadonly();
  readonly filters = this.filtersState.asReadonly();
  readonly hasFilters = computed(() =>
    Object.values(this.filtersState()).some((v) => v !== undefined && v !== ''),
  );
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
    this.api.list(page, PAGE_SIZE, this.filtersState()).subscribe({
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

  /** A new search starts from the first page. An empty text or an undefined value means "no filter". */
  filter(changes: Partial<AccountFilters>): void {
    const next = { ...this.filtersState(), ...changes };
    this.filtersState.set({
      ...(next.name?.trim() ? { name: next.name.trim() } : {}),
      ...(next.type ? { type: next.type } : {}),
      ...(next.active !== undefined ? { active: next.active } : {}),
    });
    this.load(0);
  }

  clearFilters(): void {
    this.filtersState.set({});
    this.load(0);
  }

  /** Creates the account, then reloads the current page so that the list is the server's truth. */
  create(request: CreateAccountRequest): Observable<AccountResponse> {
    return this.api.create(request).pipe(tap(() => this.load()));
  }

  update(id: string, request: UpdateAccountRequest): Observable<AccountResponse> {
    return this.api.update(id, request).pipe(tap(() => this.load()));
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
