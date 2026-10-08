import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Advice } from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the advisor feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class AdvisorApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  /** The server builds the snapshot from the trial balance (posted entries only) and asks its AI provider. */
  analyzeLedger(): Observable<Advice> {
    return this.http.post<Advice>(
      `${this.config.value.apiBaseUrl}/api/v1/advisor/analyze-ledger`,
      {},
    );
  }
}
