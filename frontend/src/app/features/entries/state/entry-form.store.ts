import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, switchMap, tap } from 'rxjs';
import {
  AccountResponse,
  EntryType,
  JournalEntry,
  JournalEntryLineRequest,
} from '../../../core/api/api-types';
import {
  Decimal,
  isPositive,
  isZero,
  normalizeTypedAmount,
  parseDecimal,
  subtract,
  sum,
  toDecimalString,
  abs,
} from '../../../shared/money/decimal';
import { EntriesApi } from '../data-access/entries.api';

export interface LineDraft {
  readonly id: number;
  readonly accountId: string;
  readonly side: EntryType;
  /** As typed: "1 500 000" and "1500,50" are accepted. */
  readonly amount: string;
}

export interface LineProblems {
  readonly account: boolean;
  readonly amount: boolean;
}

const ACCOUNTS_LOADED = 100;

/** State of the "new journal entry" screen, provided by the route so that each visit starts with an empty form. */
@Injectable()
export class EntryFormStore {
  private readonly api = inject(EntriesApi);
  private nextId = 3;

  readonly accountsStatus = signal<'loading' | 'loaded' | 'error'>('loading');
  readonly accounts = signal<readonly AccountResponse[]>([]);

  readonly description = signal('');
  readonly currency = signal('');
  readonly lines = signal<readonly LineDraft[]>([
    { id: 1, accountId: '', side: 'DEBIT', amount: '' },
    { id: 2, accountId: '', side: 'CREDIT', amount: '' },
  ]);
  /** Errors are only shown once the person has tried to save: a fresh form is not covered in red. */
  readonly submitted = signal(false);
  /** Set once the entry exists on the server, so that a failed posting can be told apart from a failed save. */
  readonly savedDraft = signal<JournalEntry | null>(null);

  /** An entry has one currency: the ledger's currencies, taken from its active accounts. */
  readonly currencies = computed(() =>
    Array.from(new Set(this.accounts().map((a) => a.currencyCode))).sort(),
  );
  readonly eligibleAccounts = computed(() =>
    this.accounts().filter((a) => a.currencyCode === this.currency()),
  );
  readonly accountNames = computed(() => new Map(this.accounts().map((a) => [a.id, a.name])));

  private readonly parsed = computed(() =>
    this.lines().map((line) => ({
      line,
      amount: parseDecimal(normalizeTypedAmount(line.amount)),
    })),
  );

  readonly problems = computed(() => {
    const eligible = new Set(this.eligibleAccounts().map((a) => a.id));
    return new Map<number, LineProblems>(
      this.parsed().map(({ line, amount }) => [
        line.id,
        {
          account: !eligible.has(line.accountId),
          amount: amount === null || !isPositive(amount),
        },
      ]),
    );
  });

  private total(side: EntryType): Decimal {
    return sum(
      this.parsed()
        .filter(({ line, amount }) => line.side === side && amount !== null)
        .map(({ amount }) => amount as Decimal),
    );
  }

  readonly totalDebits = computed(() => toDecimalString(this.total('DEBIT')));
  readonly totalCredits = computed(() => toDecimalString(this.total('CREDIT')));
  readonly gap = computed(() => subtract(this.total('DEBIT'), this.total('CREDIT')));
  readonly gapAmount = computed(() => toDecimalString(abs(this.gap())));
  readonly hasBothSides = computed(() => {
    const sides = new Set(this.lines().map((l) => l.side));
    return sides.has('DEBIT') && sides.has('CREDIT');
  });
  readonly balanced = computed(
    () => this.hasBothSides() && isPositive(this.total('DEBIT')) && isZero(this.gap()),
  );

  readonly descriptionMissing = computed(() => this.description().trim() === '');
  readonly valid = computed(
    () =>
      !this.descriptionMissing() &&
      this.currency() !== '' &&
      this.lines().length >= 2 &&
      Array.from(this.problems().values()).every((p) => !p.account && !p.amount) &&
      this.balanced(),
  );

  load(): void {
    this.accountsStatus.set('loading');
    this.api.activeAccounts(ACCOUNTS_LOADED).subscribe({
      next: (page) => {
        this.accounts.set(page.content);
        // The currency most of the accounts use is the likeliest one.
        const counts = new Map<string, number>();
        page.content.forEach((a) =>
          counts.set(a.currencyCode, (counts.get(a.currencyCode) ?? 0) + 1),
        );
        const [mostUsed] = [...counts.entries()].sort((a, b) => b[1] - a[1])[0] ?? [''];
        this.currency.set(mostUsed);
        this.accountsStatus.set('loaded');
      },
      // Already toasted by the interceptor: the screen offers a retry.
      error: () => this.accountsStatus.set('error'),
    });
  }

  setCurrency(currency: string): void {
    this.currency.set(currency);
    // An account in another currency cannot stay on a line of this entry.
    const eligible = new Set(this.eligibleAccounts().map((a) => a.id));
    this.lines.update((lines) =>
      lines.map((l) => (eligible.has(l.accountId) ? l : { ...l, accountId: '' })),
    );
  }

  addLine(side: EntryType): void {
    this.lines.update((lines) => [
      ...lines,
      { id: this.nextId++, accountId: '', side, amount: this.suggestedAmount(side) },
    ]);
  }

  /** Proposes the missing amount on the side that is short, which is what most people would type anyway. */
  private suggestedAmount(side: EntryType): string {
    const gap = this.gap();
    const shortSide: EntryType = gap.units > 0n ? 'CREDIT' : 'DEBIT';
    return !isZero(gap) && side === shortSide ? toDecimalString(abs(gap)) : '';
  }

  removeLine(id: number): void {
    if (this.lines().length > 2) {
      this.lines.update((lines) => lines.filter((l) => l.id !== id));
    }
  }

  updateLine(id: number, patch: Partial<Omit<LineDraft, 'id'>>): void {
    this.lines.update((lines) => lines.map((l) => (l.id === id ? { ...l, ...patch } : l)));
  }

  /** Marks the form as attempted, so that its errors show, and tells whether it can be sent. */
  validate(): boolean {
    this.submitted.set(true);
    return this.valid();
  }

  /** Saves the entry as a draft, and posts it right away when asked. */
  submit(post: boolean): Observable<JournalEntry> {
    const lines: JournalEntryLineRequest[] = this.parsed().map(({ line, amount }) => ({
      accountId: line.accountId,
      entryType: line.side,
      amount: toDecimalString(amount as Decimal),
    }));
    const saved = this.api
      .record({ description: this.description().trim(), currencyCode: this.currency(), lines })
      .pipe(tap((entry) => this.savedDraft.set(entry)));
    return post ? saved.pipe(switchMap((entry) => this.api.post(entry.id))) : saved;
  }
}
