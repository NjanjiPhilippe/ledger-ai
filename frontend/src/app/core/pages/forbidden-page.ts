import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-forbidden-page',
  imports: [RouterLink, TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-bold tracking-tight">{{ 'forbidden.title' | transloco }}</h1>
    <p class="mt-2 text-graphite">{{ 'forbidden.message' | transloco }}</p>
    <a
      routerLink="/"
      class="mt-4 inline-flex items-center rounded-lg bg-ink px-4 py-2 text-sm font-medium text-white hover:bg-ink-soft"
      >{{ 'forbidden.back' | transloco }}</a
    >
  `,
})
export class ForbiddenPage {}
