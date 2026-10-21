import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PagedAccounts, PagedJournalEntries, TrialBalance } from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the dashboard feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  private url(path: string): string {
    return `${this.config.value.apiBaseUrl}/api/v1${path}`;
  }

  trialBalance(): Observable<TrialBalance> {
    return this.http.get<TrialBalance>(this.url('/reports/trial-balance'));
  }

  recentEntries(size: number): Observable<PagedJournalEntries> {
    return this.http.get<PagedJournalEntries>(this.url('/journal-entries'), {
      params: new HttpParams().set('page', 0).set('size', size),
    });
  }

  accounts(size: number): Observable<PagedAccounts> {
    return this.http.get<PagedAccounts>(this.url('/accounts'), {
      params: new HttpParams().set('page', 0).set('size', size),
    });
  }
}
