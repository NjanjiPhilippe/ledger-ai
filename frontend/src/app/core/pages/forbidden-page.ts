import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-forbidden-page',
  imports: [RouterLink, TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">{{ 'forbidden.title' | transloco }}</h1>
    <p class="mt-2 text-slate-600">{{ 'forbidden.message' | transloco }}</p>
    <a routerLink="/" class="mt-4 inline-block underline">{{ 'forbidden.back' | transloco }}</a>
  `,
})
export class ForbiddenPage {}
