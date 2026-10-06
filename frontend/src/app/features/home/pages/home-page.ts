import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslocoPipe } from '@jsverse/transloco';
import { catchError, of } from 'rxjs';
import { MeResponse } from '../../../core/api/api-types';
import { AuthService } from '../../../core/auth/auth.service';
import { MeApi } from '../data-access/me.api';

@Component({
  selector: 'app-home-page',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">
      {{ 'home.title' | transloco: { name: auth.username() ?? '' } }}
    </h1>
    <p class="mt-1 text-slate-600">{{ 'home.subtitle' | transloco }}</p>

    <section class="mt-6 max-w-xl rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 class="font-medium">{{ 'home.session' | transloco }}</h2>
      <dl class="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
        <dt class="text-slate-500">{{ 'home.roles' | transloco }}</dt>
        <dd data-testid="roles">
          @for (role of auth.appRoles(); track role) {
            <span class="mr-1 inline-block rounded bg-slate-100 px-2 py-0.5">{{
              'roles.' + role | transloco
            }}</span>
          } @empty {
            {{ 'home.noRoles' | transloco }}
          }
        </dd>
        <dt class="text-slate-500">{{ 'home.userId' | transloco }}</dt>
        <dd class="break-all font-mono" data-testid="user-id">{{ me()?.userId }}</dd>
        <dt class="text-slate-500">{{ 'home.tenantId' | transloco }}</dt>
        <dd class="break-all font-mono" data-testid="tenant-id">{{ me()?.tenantId }}</dd>
      </dl>
    </section>
  `,
})
export class HomePage {
  protected readonly auth = inject(AuthService);
  private readonly meApi = inject(MeApi);

  // The error is already shown as a toast by the error interceptor: the page just stays without identifiers.
  protected readonly me = toSignal<MeResponse | null>(
    this.meApi.get().pipe(catchError(() => of(null))),
    { initialValue: null },
  );
}
