import { ChangeDetectionStrategy, Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from '../../../core/auth/auth.service';
import { hasRole } from '../../../core/auth/roles';
import { AppIcon } from '../../../shared/ui/icon';
import { EntriesListStore } from '../state/entries-list.store';
import { EntriesFilters } from '../ui/entries-filters';
import { EntriesTable } from '../ui/entries-table';

@Component({
  selector: 'app-entries-page',
  imports: [RouterLink, TranslocoPipe, AppIcon, EntriesFilters, EntriesTable],
  providers: [EntriesListStore],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="space-y-6">
      <header class="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="text-3xl font-bold tracking-tight">{{ 'entries.title' | transloco }}</h1>
          @if (store.status() === 'loaded') {
            <p class="mt-1 text-graphite" data-testid="total">
              {{ 'entries.total' | transloco: { count: store.totalElements() } }}
            </p>
          }
        </div>
        @if (canCreate()) {
          <a
            routerLink="/entries/new"
            class="inline-flex items-center gap-2 rounded-full bg-ink px-5 py-2.5 text-sm font-medium text-white shadow-card hover:bg-ink-soft"
            data-testid="new-entry"
          >
            <app-icon name="plus" class="size-4 text-mint" />
            {{ 'nav.newEntry' | transloco }}
          </a>
        }
      </header>

      <app-entries-filters
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
            <p>{{ 'entries.loadFailed' | transloco }}</p>
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
          @if (store.entries().length === 0) {
            <div
              class="flex flex-col items-center gap-2 rounded-2xl border border-dashed border-silver bg-white p-10 text-center text-graphite"
              data-testid="empty"
            >
              <app-icon name="book" class="size-8" />
              <p>{{ (store.hasFilters() ? 'entries.noMatch' : 'entries.empty') | transloco }}</p>
            </div>
          } @else {
            <app-entries-table
              [entries]="store.entries()"
              [accountNames]="store.accountNames()"
              [unknownAccount]="'common.unknownAccount' | transloco"
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
  `,
})
export class EntriesPage implements OnInit {
  protected readonly store = inject(EntriesListStore);
  private readonly auth = inject(AuthService);

  protected readonly canCreate = computed(() => hasRole(this.auth.appRoles(), 'accountant'));

  ngOnInit(): void {
    this.store.loadAccountNames();
    this.store.load(0);
  }
}
