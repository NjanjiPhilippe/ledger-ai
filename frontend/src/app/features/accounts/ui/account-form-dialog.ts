import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import {
  AccountResponse,
  AccountType,
  CreateAccountRequest,
  UpdateAccountRequest,
} from '../../../core/api/api-types';
import { AppIcon } from '../../../shared/ui/icon';

export const ACCOUNT_TYPES: readonly AccountType[] = [
  'ASSET',
  'LIABILITY',
  'EQUITY',
  'REVENUE',
  'EXPENSE',
];

/**
 * Creates an account (name, type, currency) or edits one (name and status only: the API fixes type and currency
 * once an account exists, because entries may already be posted to it). Presentation only: it emits what to save.
 */
@Component({
  selector: 'app-account-form-dialog',
  imports: [ReactiveFormsModule, TranslocoPipe, AppIcon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closed.emit()' },
  template: `
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <button
        type="button"
        class="absolute inset-0 bg-ink/40"
        tabindex="-1"
        [attr.aria-label]="'accounts.form.cancel' | transloco"
        (click)="closed.emit()"
      ></button>
      <form
        class="relative w-full max-w-md space-y-5 rounded-3xl bg-white p-6 shadow-lift"
        role="dialog"
        aria-modal="true"
        aria-labelledby="account-form-title"
        [formGroup]="form"
        (ngSubmit)="submit()"
        data-testid="account-form"
      >
        <header class="flex items-center gap-3">
          <span class="flex size-9 items-center justify-center rounded-xl bg-fog">
            <app-icon [name]="editing() ? 'landmark' : 'wallet'" class="size-[18px]" />
          </span>
          <h2 id="account-form-title" class="text-lg font-bold tracking-tight">
            {{ (editing() ? 'accounts.form.editTitle' : 'accounts.form.createTitle') | transloco }}
          </h2>
        </header>

        <div class="space-y-1.5">
          <label for="account-name" class="text-sm font-medium text-graphite">
            {{ 'accounts.form.name' | transloco }}
          </label>
          <input
            #nameInput
            id="account-name"
            type="text"
            formControlName="name"
            maxlength="120"
            autocomplete="off"
            class="w-full rounded-lg border border-silver bg-white px-3.5 py-2.5 focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
            [attr.aria-invalid]="showNameError()"
            aria-describedby="account-name-error"
          />
          @if (showNameError()) {
            <p id="account-name-error" class="text-sm text-negative" data-testid="name-error">
              {{ 'accounts.form.nameRequired' | transloco }}
            </p>
          }
        </div>

        @if (!editing()) {
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1.5">
              <label for="account-type" class="text-sm font-medium text-graphite">
                {{ 'accounts.form.type' | transloco }}
              </label>
              <select
                id="account-type"
                formControlName="type"
                class="w-full rounded-lg border border-silver bg-white px-3 py-2.5 focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
              >
                @for (type of types; track type) {
                  <option [value]="type">{{ 'accountType.' + type | transloco }}</option>
                }
              </select>
            </div>
            <div class="space-y-1.5">
              <label for="account-currency" class="text-sm font-medium text-graphite">
                {{ 'accounts.form.currency' | transloco }}
              </label>
              <input
                id="account-currency"
                type="text"
                formControlName="currencyCode"
                maxlength="3"
                autocomplete="off"
                class="w-full rounded-lg border border-silver bg-white px-3.5 py-2.5 uppercase tabular-nums focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
                [attr.aria-invalid]="showCurrencyError()"
                aria-describedby="account-currency-error"
              />
            </div>
          </div>
          @if (showCurrencyError()) {
            <p
              id="account-currency-error"
              class="-mt-2 text-sm text-negative"
              data-testid="currency-error"
            >
              {{ 'accounts.form.currencyInvalid' | transloco }}
            </p>
          }
        } @else {
          <label class="flex items-center gap-3 rounded-xl bg-fog p-3">
            <input type="checkbox" formControlName="active" class="size-4 accent-ink" />
            <span>
              <span class="block text-sm font-medium">{{
                'accounts.form.active' | transloco
              }}</span>
              <span class="block text-xs text-graphite">{{
                'accounts.form.activeHint' | transloco
              }}</span>
            </span>
          </label>
        }

        <footer class="flex justify-end gap-2 pt-1">
          <button
            type="button"
            class="rounded-lg border border-silver bg-white px-4 py-2 text-sm font-medium hover:bg-fog"
            (click)="closed.emit()"
          >
            {{ 'accounts.form.cancel' | transloco }}
          </button>
          <button
            type="submit"
            class="rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white shadow-card hover:bg-ink-soft disabled:opacity-60"
            [disabled]="saving()"
            data-testid="save"
          >
            {{ 'accounts.form.save' | transloco }}
          </button>
        </footer>
      </form>
    </div>
  `,
})
export class AccountFormDialog {
  /** The account being edited, or null to create a new one. */
  readonly account = input<AccountResponse | null>(null);
  /** Currency proposed for a new account: the one already used by the ledger. */
  readonly defaultCurrency = input('XAF');
  readonly saving = input(false);

  readonly create = output<CreateAccountRequest>();
  readonly update = output<{ id: string; request: UpdateAccountRequest }>();
  readonly closed = output<void>();

  protected readonly types = ACCOUNT_TYPES;
  protected readonly editing = computed(() => this.account() !== null);

  private readonly nameInput = viewChild<ElementRef<HTMLInputElement>>('nameInput');
  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(/\S/)]],
    type: ['ASSET' as AccountType],
    currencyCode: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    active: [true],
  });

  constructor() {
    effect(() => {
      const account = this.account();
      this.form.reset({
        name: account?.name ?? '',
        type: account?.type ?? 'ASSET',
        currencyCode: account?.currencyCode ?? this.defaultCurrency(),
        active: account?.active ?? true,
      });
    });
    effect(() => this.nameInput()?.nativeElement.focus());
  }

  protected showNameError(): boolean {
    const control = this.form.controls.name;
    return control.invalid && (control.touched || control.dirty);
  }

  protected showCurrencyError(): boolean {
    const control = this.form.controls.currencyCode;
    return control.invalid && (control.touched || control.dirty);
  }

  protected submit(): void {
    const account = this.account();
    const { name, type, currencyCode, active } = this.form.getRawValue();
    this.form.controls.name.markAsTouched();
    this.form.controls.currencyCode.markAsTouched();
    if (this.saving() || this.form.controls.name.invalid) {
      return;
    }
    if (account) {
      this.update.emit({ id: account.id, request: { name: name.trim(), active } });
      return;
    }
    if (this.form.controls.currencyCode.invalid) {
      return;
    }
    this.create.emit({ name: name.trim(), type, currencyCode: currencyCode.toUpperCase() });
  }
}
