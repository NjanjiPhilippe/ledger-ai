import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AccountFilters,
  AccountResponse,
  CreateAccountRequest,
  PagedAccounts,
  UpdateAccountRequest,
} from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the accounts feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class AccountsApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  private url(path = ''): string {
    return `${this.config.value.apiBaseUrl}/api/v1/accounts${path}`;
  }

  list(page: number, size: number, filters: AccountFilters = {}): Observable<PagedAccounts> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.name) {
      params = params.set('name', filters.name);
    }
    if (filters.type) {
      params = params.set('type', filters.type);
    }
    if (filters.active !== undefined) {
      params = params.set('active', filters.active);
    }
    return this.http.get<PagedAccounts>(this.url(), { params });
  }

  create(request: CreateAccountRequest): Observable<AccountResponse> {
    return this.http.post<AccountResponse>(this.url(), request);
  }

  update(id: string, request: UpdateAccountRequest): Observable<AccountResponse> {
    return this.http.put<AccountResponse>(this.url(`/${id}`), request);
  }
}
