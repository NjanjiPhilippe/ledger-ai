import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { finalize } from 'rxjs';
import { ToastService } from '../../../core/toast/toast.service';
import { AppIcon } from '../../../shared/ui/icon';
import { EntryFormStore } from '../state/entry-form.store';
import { EntryLinesEditor } from '../ui/entry-lines-editor';
import { EntryTotals } from '../ui/entry-totals';

@Component({
  selector: 'app-new-entry-page',
  imports: [RouterLink, TranslocoPipe, AppIcon, EntryLinesEditor, EntryTotals],
  providers: [EntryFormStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header>
        <h1 class="text-3xl font-bold tracking-tight">{{ 'entry.title' | transloco }}</h1>
        <p class="mt-1 text-graphite">{{ 'entry.subtitle' | transloco }}</p>
      </header>

      @switch (store.accountsStatus()) {
        @case ('error') {
          <div
            class="rounded-2xl bg-negative-soft p-4 text-negative"
            role="alert"
            data-testid="error"
          >
            <p>{{ 'entry.accountsFailed' | transloco }}</p>
            <button
              type="button"
              class="mt-2 rounded-lg bg-ink px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-soft"
              (click)="store.load()"
            >
              {{ 'common.retry' | transloco }}
            </button>
          </div>
        }
        @case ('loading') {
          <p class="text-graphite" role="status" data-testid="loading">
            {{ 'common.loading' | transloco }}
          </p>
        }
        @default {
          @if (store.accounts().length < 2) {
            <div
              class="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
              data-testid="no-accounts"
            >
              <app-icon name="landmark" class="size-8" />
              <p>{{ 'entry.noAccounts' | transloco }}</p>
              <a
                routerLink="/accounts"
                class="rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white hover:bg-ink-soft"
              >
                {{ 'nav.accounts' | transloco }}
              </a>
            </div>
          } @else {
            <form class="space-y-6" novalidate (submit)="$event.preventDefault()">
              <section class="space-y-4 rounded-2xl border border-mist bg-white p-5 shadow-card">
                <h2 class="text-base font-bold tracking-tight">
                  {{ 'entry.details' | transloco }}
                </h2>
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-[minmax(0,1fr)_10rem]">
                  <div class="space-y-1.5">
                    <label for="entry-description" class="text-sm font-medium text-graphite">
                      {{ 'entry.description' | transloco }}
                    </label>
                    <input
                      id="entry-description"
                      type="text"
                      maxlength="200"
                      autocomplete="off"
                      class="w-full rounded-lg border bg-white px-3.5 py-2.5 focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
                      [class]="
                        store.submitted() && store.descriptionMissing()
                          ? 'border-negative'
                          : 'border-silver'
                      "
                      [placeholder]="'entry.descriptionPlaceholder' | transloco"
                      [value]="store.description()"
                      (input)="store.description.set($any($event.target).value)"
                    />
                    @if (store.submitted() && store.descriptionMissing()) {
                      <p class="text-sm text-negative" data-testid="description-error">
                        {{ 'entry.descriptionRequired' | transloco }}
                      </p>
                    }
                  </div>
                  <div class="space-y-1.5">
                    <label for="entry-currency" class="text-sm font-medium text-graphite">
                      {{ 'entry.currency' | transloco }}
                    </label>
                    <select
                      id="entry-currency"
                      class="w-full rounded-lg border border-silver bg-white px-3 py-2.5 focus:border-mint focus:outline-none focus:ring-2 focus:ring-mint"
                      (change)="store.setCurrency($any($event.target).value)"
                    >
                      @for (currency of store.currencies(); track currency) {
                        <option [value]="currency" [selected]="currency === store.currency()">
                          {{ currency }}
                        </option>
                      }
                    </select>
                  </div>
                </div>
              </section>

              <section class="space-y-4 rounded-2xl border border-mist bg-white p-5 shadow-card">
                <h2 class="text-base font-bold tracking-tight">{{ 'entry.lines' | transloco }}</h2>
                <app-entry-lines-editor
                  [lines]="store.lines()"
                  [accounts]="store.eligibleAccounts()"
                  [issues]="store.problems()"
                  [showIssues]="store.submitted()"
                  (lineChange)="store.updateLine($event.id, $event.patch)"
                  (add)="store.addLine($event)"
                  (remove)="store.removeLine($event)"
                />
                <app-entry-totals
                  [debits]="store.totalDebits()"
                  [credits]="store.totalCredits()"
                  [gap]="store.gapAmount()"
                  [balanced]="store.balanced()"
                  [currency]="store.currency()"
                />
                @if (store.submitted() && !store.valid() && !store.balanced()) {
                  <p class="text-sm text-negative" role="alert" data-testid="balance-error">
                    {{
                      (store.hasBothSides() ? 'entry.errors.unbalanced' : 'entry.errors.sides')
                        | transloco
                    }}
                  </p>
                }
              </section>

              <footer class="flex flex-wrap justify-end gap-3">
                <a
                  routerLink="/"
                  class="rounded-full border border-silver bg-white px-5 py-2.5 text-sm font-medium hover:bg-fog"
                >
                  {{ 'entry.cancel' | transloco }}
                </a>
                <button
                  type="button"
                  class="rounded-full border border-ink bg-white px-5 py-2.5 text-sm font-medium text-ink hover:bg-fog disabled:opacity-60"
                  [disabled]="saving()"
                  (click)="save(false)"
                  data-testid="save-draft"
                >
                  {{ 'entry.saveDraft' | transloco }}
                </button>
                <button
                  type="button"
                  class="rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white shadow-card hover:bg-ink-soft disabled:opacity-60"
                  [disabled]="saving()"
                  (click)="save(true)"
                  data-testid="save-post"
                >
                  {{ 'entry.savePost' | transloco }}
                </button>
              </footer>
            </form>
          }
        }
      }
    </div>
  `,
})
export class NewEntryPage implements OnInit {
  protected readonly store = inject(EntryFormStore);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  protected readonly saving = signal(false);

  ngOnInit(): void {
    this.store.load();
  }

  protected save(post: boolean): void {
    if (this.saving() || !this.store.validate()) {
      return;
    }
    const description = this.store.description().trim();
    this.saving.set(true);
    this.store
      .submit(post)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          const key = post ? 'entry.posted' : 'entry.draftSaved';
          this.toasts.success(this.transloco.translate(key, { description }));
          void this.router.navigateByUrl('/');
        },
        error: () => {
          // The error itself is toasted by the interceptor. When only the posting failed, the draft exists:
          // say so, and leave the form, so that the same entry is not saved twice.
          if (post && this.store.savedDraft()) {
            this.toasts.info(this.transloco.translate('entry.postFailed', { description }));
            void this.router.navigateByUrl('/');
          }
        },
      });
  }
}
