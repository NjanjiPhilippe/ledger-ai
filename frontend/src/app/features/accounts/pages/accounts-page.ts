import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import {
  AccountResponse,
  CreateAccountRequest,
  UpdateAccountRequest,
} from '../../../core/api/api-types';
import { AuthService } from '../../../core/auth/auth.service';
import { hasRole } from '../../../core/auth/roles';
import { ToastService } from '../../../core/toast/toast.service';
import { AppIcon } from '../../../shared/ui/icon';
import { AccountsStore } from '../state/accounts.store';
import { AccountFormDialog } from '../ui/account-form-dialog';
import { AccountsFilters } from '../ui/accounts-filters';
import { AccountsTable } from '../ui/accounts-table';

@Component({
  selector: 'app-accounts-page',
  imports: [AccountsTable, AccountsFilters, AccountFormDialog, AppIcon, TranslocoPipe],
  providers: [AccountsStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="text-3xl font-bold tracking-tight">{{ 'accounts.title' | transloco }}</h1>
          @if (store.status() === 'loaded') {
            <p class="mt-1 text-graphite" data-testid="total">
              {{ 'accounts.total' | transloco: { count: store.totalElements() } }}
            </p>
          }
        </div>
        @if (canCreate()) {
          <button
            type="button"
            class="inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white shadow-card hover:bg-ink-soft focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-mint"
            (click)="openCreate()"
            data-testid="new-account"
          >
            <app-icon name="plus" class="size-4 text-mint" />
            {{ 'accounts.new' | transloco }}
          </button>
        }
      </header>

      <app-accounts-filters
        [filters]="store.filters()"
        [hasFilters]="store.hasFilters()"
        (filterChange)="store.filter($event)"
        (clear)="store.clearFilters()"
      />

      @switch (store.status()) {
        @case ('error') {
          <div
            class="rounded-2xl bg-negative-soft p-4 text-negative"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'accounts.loadFailed' | transloco }}</p>
            <button
              type="button"
              class="mt-2 rounded-lg bg-ink px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-soft"
              (click)="store.load()"
            >
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @case ('loaded') {
          @if (store.accounts().length === 0) {
            <div
              class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
              data-testid="empty"
            >
              <app-icon name="inbox" class="size-8" />
              <p>
                {{ (store.hasFilters() ? 'accounts.noMatch' : 'accounts.empty') | transloco }}
              </p>
            </div>
          } @else {
            <app-accounts-table
              [accounts]="store.accounts()"
              [canEdit]="canEdit()"
              (edit)="openEdit($event)"
            />
            <nav class="flex items-center justify-between text-sm" aria-label="pagination">
              <button
                type="button"
                class="rounded-full border border-silver bg-white px-4 py-2 font-medium hover:bg-fog disabled:opacity-50"
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
                class="rounded-full border border-silver bg-white px-4 py-2 font-medium hover:bg-fog disabled:opacity-50"
                [disabled]="!store.hasNext()"
                (click)="store.next()"
              >
                {{ 'common.next' | transloco }}
              </button>
            </nav>
          }
        }
        @default {
          <p class="text-graphite" role="status" data-testid="loading">
            {{ 'common.loading' | transloco }}
          </p>
        }
      }
    </div>

    @if (dialogOpen()) {
      <app-account-form-dialog
        [account]="editing()"
        [defaultCurrency]="defaultCurrency()"
        [saving]="saving()"
        (create)="create($event)"
        (update)="update($event.id, $event.request)"
        (closed)="closeDialog()"
      />
    }
  `,
})
export class AccountsPage implements OnInit {
  protected readonly store = inject(AccountsStore);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  protected readonly dialogOpen = signal(false);
  protected readonly editing = signal<AccountResponse | null>(null);
  protected readonly saving = signal(false);

  // Same rules as the backend: an accountant creates accounts, only an administrator changes one.
  protected readonly canCreate = computed(() => hasRole(this.auth.appRoles(), 'accountant'));
  protected readonly canEdit = computed(() => hasRole(this.auth.appRoles(), 'admin'));
  protected readonly defaultCurrency = computed(
    () => this.store.accounts()[0]?.currencyCode ?? 'XAF',
  );

  ngOnInit(): void {
    this.store.load(0);
  }

  protected openCreate(): void {
    this.editing.set(null);
    this.dialogOpen.set(true);
  }

  protected openEdit(account: AccountResponse): void {
    this.editing.set(account);
    this.dialogOpen.set(true);
  }

  protected closeDialog(): void {
    this.dialogOpen.set(false);
  }

  protected create(request: CreateAccountRequest): void {
    this.saving.set(true);
    this.store
      .create(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (account) => {
          this.toasts.success(this.transloco.translate('accounts.created', { name: account.name }));
          this.closeDialog();
        },
        // The failure is toasted by the interceptor; the dialog stays open so that nothing typed is lost.
        error: () => undefined,
      });
  }

  protected update(id: string, request: UpdateAccountRequest): void {
    this.saving.set(true);
    this.store
      .update(id, request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (account) => {
          this.toasts.success(this.transloco.translate('accounts.updated', { name: account.name }));
          this.closeDialog();
        },
        error: () => undefined,
      });
  }
}
