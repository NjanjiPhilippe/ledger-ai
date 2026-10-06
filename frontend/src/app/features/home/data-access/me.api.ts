import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MeResponse } from '../../../core/api/api-types';
import { AppConfigService } from '../../../core/config/app-config';

@Injectable({ providedIn: 'root' })
export class MeApi {
  private readonly http = inject(HttpClient);
  private readonly config = inject(AppConfigService);

  get(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.config.value.apiBaseUrl}/api/v1/me`);
  }
}
