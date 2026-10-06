import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PagedAccounts } from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the accounts feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class AccountsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  list(page: number, size: number): Observable<PagedAccounts> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedAccounts>(`${this.config.value.apiBaseUrl}/api/v1/accounts`, {
      params,
    });
  }
}
