import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AccountResponse, JournalEntry } from '../../../core/api/api-types';
import {
  Decimal,
  isZero,
  parseDecimal,
  subtract,
  sum,
  toDecimalString,
} from '../../../shared/money/decimal';
import { EntriesApi } from '../data-access/entries.api';

export type DetailStatus = 'loading' | 'loaded' | 'notFound' | 'error';

export interface DetailLine {
  readonly accountName: string | null;
  readonly side: 'DEBIT' | 'CREDIT';
  readonly amount: string;
}

const ACCOUNTS_LOADED = 200;

/** State of one journal entry: the entry, the names of its accounts, and the two things that can be done to it. */
@Injectable()
export class EntryDetailStore {
  private readonly api = inject(EntriesApi);

  readonly status = signal<DetailStatus>('loading');
  readonly entry = signal<JournalEntry | null>(null);
  /** The entry this one reverses, when it is a reversal. */
  readonly original = signal<JournalEntry | null>(null);
  private readonly accounts = signal<readonly AccountResponse[]>([]);
  /** Which entry was asked for last: an older, slower answer must not replace it. */
  private requested = '';

  readonly lines = computed<DetailLine[]>(() => {
    const names = new Map(this.accounts().map((a) => [a.id, a.name]));
    return (this.entry()?.lines ?? []).map((line) => ({
      accountName: names.get(line.accountId) ?? null,
      side: line.entryType,
      amount: line.amount,
    }));
  });

  /** All the lines of an entry are in one currency, which is that of its accounts. */
  readonly currency = computed(() => {
    const first = this.entry()?.lines[0];
    return this.accounts().find((a) => a.id === first?.accountId)?.currencyCode ?? '';
  });

  private total(side: 'DEBIT' | 'CREDIT'): string {
    const amounts = (this.entry()?.lines ?? [])
      .filter((l) => l.entryType === side)
      .map((l) => parseDecimal(l.amount))
      .filter((v): v is Decimal => v !== null);
    return toDecimalString(sum(amounts));
  }

  readonly totalDebits = computed(() => this.total('DEBIT'));
  readonly totalCredits = computed(() => this.total('CREDIT'));
  readonly balanced = computed(() => {
    const debits = parseDecimal(this.totalDebits());
    const credits = parseDecimal(this.totalCredits());
    return debits !== null && credits !== null && isZero(subtract(debits, credits));
  });

  load(id: string): void {
    this.requested = id;
    this.entry.set(null);
    this.status.set('loading');
    this.api.accounts(ACCOUNTS_LOADED).subscribe({
      next: (page) => id === this.requested && this.accounts.set(page.content),
      // Names are a nicety: the entry shows with "unknown account" if they cannot be loaded.
      error: () => undefined,
    });
    this.api.get(id).subscribe({
      next: (entry) => {
        if (id !== this.requested) {
          return;
        }
        this.entry.set(entry);
        this.status.set('loaded');
        this.loadOriginal(entry);
      },
      error: (error: { status?: number }) => {
        if (id === this.requested) {
          this.status.set(error?.status === 404 ? 'notFound' : 'error');
        }
      },
    });
  }

  private loadOriginal(entry: JournalEntry): void {
    this.original.set(null);
    if (entry.reversalOfId) {
      this.api.get(entry.reversalOfId).subscribe({
        next: (original) => entry.id === this.requested && this.original.set(original),
        error: () => undefined,
      });
    }
  }

  /** Posts a draft; the entry on screen becomes the posted one. */
  post(): Observable<JournalEntry> {
    const entry = this.entry();
    return this.api.post(entry?.id ?? '').pipe(tap((posted) => this.entry.set(posted)));
  }

  /** Reverses a posted entry. What comes back is the reversal, a new entry the screen should go to. */
  reverse(): Observable<JournalEntry> {
    const entry = this.entry();
    return this.api.reverse(entry?.id ?? '');
  }
}
