import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EntryFilters,
  JournalEntry,
  PagedAccounts,
  PagedJournalEntries,
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

  /** Every account, active or not: an old entry may name an account that has been deactivated since. */
  accounts(size: number): Observable<PagedAccounts> {
    const params = new HttpParams().set('page', 0).set('size', size);
    return this.http.get<PagedAccounts>(this.url('/accounts'), { params });
  }

  list(page: number, size: number, filters: EntryFilters = {}): Observable<PagedJournalEntries> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.createdFrom) {
      params = params.set('createdFrom', filters.createdFrom);
    }
    if (filters.createdTo) {
      params = params.set('createdTo', filters.createdTo);
    }
    return this.http.get<PagedJournalEntries>(this.url('/journal-entries'), { params });
  }

  get(id: string): Observable<JournalEntry> {
    return this.http.get<JournalEntry>(this.url(`/journal-entries/${id}`));
  }

  /** The reversal is a new entry, posted at once: it is what comes back. */
  reverse(id: string): Observable<JournalEntry> {
    return this.http.post<JournalEntry>(this.url(`/journal-entries/${id}/reverse`), {});
  }

  record(request: RecordJournalEntryRequest): Observable<JournalEntry> {
    return this.http.post<JournalEntry>(this.url('/journal-entries'), request);
  }

  post(id: string): Observable<JournalEntry> {
    return this.http.post<JournalEntry>(this.url(`/journal-entries/${id}/post`), {});
  }
}
