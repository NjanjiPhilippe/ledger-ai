import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { map } from 'rxjs';
import { finalize } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { hasRole } from '../../../core/auth/roles';
import { ToastService } from '../../../core/toast/toast.service';
import { ShortDatePipe } from '../../../shared/date/short-date.pipe';
import { MoneyPipe } from '../../../shared/money/money.pipe';
import { ConfirmDialog } from '../../../shared/ui/confirm-dialog';
import { EntryStatus } from '../../../shared/ui/entry-status';
import { AppIcon } from '../../../shared/ui/icon';
import { EntryDetailStore } from '../state/entry-detail.store';

type Action = 'post' | 'reverse';

@Component({
  selector: 'app-entry-detail-page',
  imports: [
    RouterLink,
    TranslocoPipe,
    ShortDatePipe,
    MoneyPipe,
    AppIcon,
    EntryStatus,
    ConfirmDialog,
  ],
  providers: [EntryDetailStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <nav class="flex items-center gap-2 text-sm text-graphite" aria-label="breadcrumb">
        <a routerLink="/entries" class="hover:text-ink hover:underline">{{
          'entries.title' | transloco
        }}</a>
        <app-icon name="arrowUpRight" class="size-3 rotate-45" />
        <span class="truncate text-ink">{{ store.entry()?.description }}</span>
      </nav>

      @switch (store.status()) {
        @case ('notFound') {
          <div
            class="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
            data-testid="not-found"
          >
            <app-icon name="book" class="size-8" />
            <p>{{ 'entries.notFound' | transloco }}</p>
            <a
              routerLink="/entries"
              class="rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white hover:bg-ink-soft"
            >
              {{ 'entries.back' | transloco }}
            </a>
          </div>
        }
        @case ('error') {
          <div
            class="rounded-2xl bg-negative-soft p-4 text-negative"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'entries.detailFailed' | transloco }}</p>
            <button
              type="button"
              class="mt-2 rounded-lg bg-ink px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-soft"
              (click)="reload()"
            >
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @case ('loaded') {
          @if (store.entry(); as entry) {
            <header class="flex flex-wrap items-start justify-between gap-4">
              <div class="min-w-0 space-y-2">
                <h1 class="text-3xl font-bold tracking-tight" data-testid="title">
                  {{ entry.description }}
                </h1>
                <app-entry-status [status]="entry.status" />
              </div>
              <div class="flex flex-wrap gap-2">
                @if (canPost()) {
                  <button
                    type="button"
                    class="inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white shadow-card hover:bg-ink-soft"
                    (click)="ask('post')"
                    data-testid="post"
                  >
                    <app-icon name="checkCircle" class="size-4 text-mint" />
                    {{ 'entries.post' | transloco }}
                  </button>
                }
                @if (canReverse()) {
                  <button
                    type="button"
                    class="inline-flex items-center gap-2 rounded-full border border-negative bg-white px-5 py-2.5 text-sm font-medium text-negative hover:bg-negative-soft"
                    (click)="ask('reverse')"
                    data-testid="reverse"
                  >
                    <app-icon name="refresh" class="size-4" />
                    {{ 'entries.reverse' | transloco }}
                  </button>
                }
              </div>
            </header>

            <section class="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                  {{ 'entries.created' | transloco }}
                </p>
                <p class="mt-1 text-lg font-bold" data-testid="created">
                  {{ entry.createdAt | shortDate }}
                </p>
              </div>
              <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                  {{ 'entries.posted' | transloco }}
                </p>
                <p class="mt-1 text-lg font-bold" data-testid="posted">
                  @if (entry.postedAt) {
                    {{ entry.postedAt | shortDate }}
                  } @else {
                    <span class="font-medium text-graphite">{{
                      'entries.notPosted' | transloco
                    }}</span>
                  }
                </p>
              </div>
              @if (entry.reversalOfId) {
                <div class="rounded-2xl border border-mist bg-white p-5 shadow-card">
                  <p class="text-xs font-medium uppercase tracking-wider text-graphite">
                    {{ 'entries.reversalOf' | transloco }}
                  </p>
                  <a
                    [routerLink]="['/entries', entry.reversalOfId]"
                    class="mt-1 block truncate text-lg font-bold underline decoration-silver underline-offset-4 hover:decoration-ink"
                    data-testid="original"
                  >
                    {{ store.original()?.description ?? ('entries.original' | transloco) }}
                  </a>
                </div>
              }
            </section>

            <section class="overflow-hidden rounded-2xl border border-mist bg-white shadow-card">
              <h2 class="border-b border-mist px-5 py-4 text-base font-bold tracking-tight">
                {{ 'entry.lines' | transloco }}
              </h2>
              <div class="overflow-x-auto">
                <table class="min-w-full text-sm">
                  <thead class="text-left text-xs uppercase tracking-wider text-graphite">
                    <tr>
                      <th scope="col" class="px-5 py-3 font-medium">
                        {{ 'entry.account' | transloco }}
                      </th>
                      <th scope="col" class="px-5 py-3 text-right font-medium">
                        {{ 'entry.debit' | transloco }}
                      </th>
                      <th scope="col" class="px-5 py-3 text-right font-medium">
                        {{ 'entry.credit' | transloco }}
                      </th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-mist">
                    @for (line of store.lines(); track $index) {
                      <tr data-testid="line">
                        <td class="px-5 py-3 font-medium" data-testid="line-account">
                          {{ line.accountName ?? ('common.unknownAccount' | transloco) }}
                        </td>
                        <td class="px-5 py-3 text-right tabular-nums" data-testid="line-debit">
                          @if (line.side === 'DEBIT') {
                            {{ line.amount | money: store.currency() }}
                          }
                        </td>
                        <td class="px-5 py-3 text-right tabular-nums" data-testid="line-credit">
                          @if (line.side === 'CREDIT') {
                            {{ line.amount | money: store.currency() }}
                          }
                        </td>
                      </tr>
                    }
                  </tbody>
                  <tfoot class="border-t-2 border-ink font-bold">
                    <tr data-testid="totals">
                      <th scope="row" class="px-5 py-3 text-left">
                        <span class="inline-flex items-center gap-2">
                          {{ 'dashboard.trialBalance.total' | transloco }}
                          @if (store.balanced()) {
                            <span
                              class="inline-flex items-center gap-1 rounded-full bg-positive-soft px-2.5 py-0.5 text-xs font-medium text-positive"
                            >
                              <app-icon name="checkCircle" class="size-3" />
                              {{ 'entry.totals.balanced' | transloco }}
                            </span>
                          }
                        </span>
                      </th>
                      <td class="px-5 py-3 text-right tabular-nums" data-testid="total-debits">
                        {{ store.totalDebits() | money: store.currency() }}
                      </td>
                      <td class="px-5 py-3 text-right tabular-nums" data-testid="total-credits">
                        {{ store.totalCredits() | money: store.currency() }}
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </section>
          }
        }
        @default {
          <p class="text-graphite" role="status" data-testid="loading">
            {{ 'common.loading' | transloco }}
          </p>
        }
      }
    </div>

    @if (asking(); as action) {
      <app-confirm-dialog
        [title]="
          (action === 'post' ? 'entries.confirmPost.title' : 'entries.confirmReverse.title')
            | transloco
        "
        [message]="
          (action === 'post' ? 'entries.confirmPost.message' : 'entries.confirmReverse.message')
            | transloco
        "
        [confirmLabel]="(action === 'post' ? 'entries.post' : 'entries.reverse') | transloco"
        [danger]="action === 'reverse'"
        [busy]="busy()"
        (confirmed)="confirm(action)"
        (dismissed)="asking.set(null)"
      />
    }
  `,
})
export class EntryDetailPage {
  protected readonly store = inject(EntryDetailStore);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly transloco = inject(TranslocoService);
  private readonly router = inject(Router);

  private readonly id = toSignal(
    inject(ActivatedRoute).paramMap.pipe(map((params) => params.get('id'))),
    { initialValue: null },
  );

  protected readonly asking = signal<Action | null>(null);
  protected readonly busy = signal(false);

  protected readonly canPost = computed(
    () => this.store.entry()?.status === 'DRAFT' && hasRole(this.auth.appRoles(), 'accountant'),
  );
  protected readonly canReverse = computed(
    () => this.store.entry()?.status === 'POSTED' && hasRole(this.auth.appRoles(), 'admin'),
  );

  constructor() {
    effect(() => {
      const id = this.id();
      if (id) {
        this.store.load(id);
      }
    });
  }

  protected reload(): void {
    const id = this.id();
    if (id) {
      this.store.load(id);
    }
  }

  protected ask(action: Action): void {
    this.asking.set(action);
  }

  protected confirm(action: Action): void {
    const description = this.store.entry()?.description ?? '';
    this.busy.set(true);
    if (action === 'post') {
      this.store
        .post()
        .pipe(finalize(() => this.done()))
        .subscribe({
          next: () =>
            this.toasts.success(this.transloco.translate('entries.postedToast', { description })),
          error: () => undefined,
        });
      return;
    }
    this.store
      .reverse()
      .pipe(finalize(() => this.done()))
      .subscribe({
        next: (reversal) => {
          this.toasts.success(this.transloco.translate('entries.reversedToast', { description }));
          void this.router.navigate(['/entries', reversal.id]);
        },
        error: () => undefined,
      });
  }

  private done(): void {
    this.busy.set(false);
    this.asking.set(null);
  }
}
