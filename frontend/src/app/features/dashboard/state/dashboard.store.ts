import { computed, inject, Injectable, signal, WritableSignal } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AccountType,
  PagedAccounts,
  PagedJournalEntries,
  TrialBalance,
} from '../../../core/api/api-types';
import { DashboardApi } from '../data-access/dashboard.api';

export type LoadStatus = 'loading' | 'loaded' | 'error';

export interface Loadable<T> {
  readonly status: LoadStatus;
  readonly data: T | null;
}

const LOADING = { status: 'loading', data: null } as const;
const RECENT_ENTRIES = 5;
const ACCOUNTS_SHOWN = 100;

/**
 * The three sections of the dashboard load independently: one that fails (an unavailable report, a role that
 * cannot see it) shows its own retry and does not take the others down. Errors are already toasted by the interceptor.
 */
@Injectable()
export class DashboardStore {
  private readonly api = inject(DashboardApi);

  readonly trialBalance = signal<Loadable<TrialBalance>>(LOADING);
  readonly entries = signal<Loadable<PagedJournalEntries>>(LOADING);
  readonly accounts = signal<Loadable<PagedAccounts>>(LOADING);

  /** Id to name: the dashboard shows people's words, never identifiers. */
  readonly accountNames = computed(() => {
    const names = new Map<string, string>();
    this.accounts().data?.content.forEach((a) => names.set(a.id, a.name));
    this.trialBalance().data?.lines.forEach((l) => names.set(l.accountId, l.accountName));
    return names;
  });

  readonly activeAccounts = computed(
    () => this.accounts().data?.content.filter((a) => a.active).length ?? 0,
  );
  readonly totalAccounts = computed(() => this.accounts().data?.totalElements ?? 0);

  readonly accountsByType = computed(() => {
    const counts = new Map<AccountType, number>();
    this.accounts().data?.content.forEach((a) => counts.set(a.type, (counts.get(a.type) ?? 0) + 1));
    return counts;
  });

  load(): void {
    this.loadTrialBalance();
    this.loadEntries();
    this.loadAccounts();
  }

  loadTrialBalance(): void {
    this.fetch(this.api.trialBalance(), this.trialBalance);
  }

  loadEntries(): void {
    this.fetch(this.api.recentEntries(RECENT_ENTRIES), this.entries);
  }

  loadAccounts(): void {
    this.fetch(this.api.accounts(ACCOUNTS_SHOWN), this.accounts);
  }

  private fetch<T>(source: Observable<T>, target: WritableSignal<Loadable<T>>): void {
    target.set(LOADING);
    source.subscribe({
      next: (data) => target.set({ status: 'loaded', data }),
      error: () => target.set({ status: 'error', data: null }),
    });
  }
}
