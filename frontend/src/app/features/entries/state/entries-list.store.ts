import { computed, inject, Injectable, signal } from '@angular/core';
import { AccountResponse, EntryFilters, PagedJournalEntries } from '../../../core/api/api-types';
import { EntriesApi } from '../data-access/entries.api';

export type ListStatus = 'idle' | 'loading' | 'loaded' | 'error';

const PAGE_SIZE = 20;
const ACCOUNTS_LOADED = 200;

/** State of the journal entries list, provided by the route so that it starts fresh each time the screen opens. */
@Injectable()
export class EntriesListStore {
  private readonly api = inject(EntriesApi);

  private readonly pageState = signal<PagedJournalEntries | null>(null);
  private readonly statusState = signal<ListStatus>('idle');
  private readonly filtersState = signal<EntryFilters>({});
  private readonly accountList = signal<readonly AccountResponse[]>([]);
  private requested = 0;

  readonly status = this.statusState.asReadonly();
  readonly filters = this.filtersState.asReadonly();
  readonly hasFilters = computed(() => Object.keys(this.filtersState()).length > 0);
  readonly entries = computed(() => this.pageState()?.content ?? []);
  readonly pageIndex = computed(() => this.pageState()?.page ?? this.requested);
  readonly totalPages = computed(() => this.pageState()?.totalPages ?? 0);
  readonly totalElements = computed(() => this.pageState()?.totalElements ?? 0);
  readonly hasPrevious = computed(() => this.pageIndex() > 0);
  readonly hasNext = computed(() => this.pageIndex() + 1 < this.totalPages());
  /** Id to name: the list shows people's words, never identifiers. */
  readonly accountNames = computed(() => new Map(this.accountList().map((a) => [a.id, a.name])));

  load(page = this.requested): void {
    this.requested = page;
    this.statusState.set('loading');
    this.api.list(page, PAGE_SIZE, this.filtersState()).subscribe({
      next: (result) => {
        // A slower, older response must not overwrite the page the user asked for last.
        if (page === this.requested) {
          this.pageState.set(result);
          this.statusState.set('loaded');
        }
      },
      error: () => {
        if (page === this.requested) {
          this.statusState.set('error');
        }
      },
    });
  }

  /** The names are a nicety: when they cannot be loaded the list still shows, with "unknown account". */
  loadAccountNames(): void {
    this.api.accounts(ACCOUNTS_LOADED).subscribe({
      next: (page) => this.accountList.set(page.content),
      error: () => undefined,
    });
  }

  filter(changes: Partial<EntryFilters>): void {
    const next = { ...this.filtersState(), ...changes };
    this.filtersState.set({
      ...(next.status ? { status: next.status } : {}),
      ...(next.createdFrom ? { createdFrom: next.createdFrom } : {}),
      ...(next.createdTo ? { createdTo: next.createdTo } : {}),
    });
    this.load(0);
  }

  clearFilters(): void {
    this.filtersState.set({});
    this.load(0);
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
