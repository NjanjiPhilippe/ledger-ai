import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  JournalEntry,
  PagedAccounts,
  RecordJournalEntryRequest,
} from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the entries feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class EntriesApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  private url(path: string): string {
    return `${this.config.value.apiBaseUrl}/api/v1${path}`;
  }

  /** Accounts an entry can be posted to: the active ones. */
  activeAccounts(size: number): Observable<PagedAccounts> {
    const params = new HttpParams().set('active', true).set('page', 0).set('size', size);
    return this.http.get<PagedAccounts>(this.url('/accounts'), { params });
  }

  record(request: RecordJournalEntryRequest): Observable<JournalEntry> {
    return this.http.post<JournalEntry>(this.url('/journal-entries'), request);
  }

  post(id: string): Observable<JournalEntry> {
    return this.http.post<JournalEntry>(this.url(`/journal-entries/${id}/post`), {});
  }
}
