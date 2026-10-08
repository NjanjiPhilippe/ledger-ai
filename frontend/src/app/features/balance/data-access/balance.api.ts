import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TrialBalance } from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

/** The only place of the balance feature that knows about HTTP. */
@Injectable({ providedIn: 'root' })
export class BalanceApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  trialBalance(): Observable<TrialBalance> {
    return this.http.get<TrialBalance>(
      `${this.config.value.apiBaseUrl}/api/v1/reports/trial-balance`,
    );
  }
}
