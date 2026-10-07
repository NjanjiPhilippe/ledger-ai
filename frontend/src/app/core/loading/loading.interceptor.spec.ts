import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppConfigService } from '../config/app-config';
import { loadingInterceptor } from './loading.interceptor';
import { LoadingService } from './loading.service';

describe('loadingInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let loading: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([loadingInterceptor])),
        provideHttpClientTesting(),
        { provide: AppConfigService, useValue: { value: { apiBaseUrl: 'http://api.test' } } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    loading = TestBed.inject(LoadingService);
  });

  it('is busy until the API call succeeds', () => {
    http.get('http://api.test/x').subscribe();
    expect(loading.busy()).toBe(true);

    backend.expectOne('http://api.test/x').flush({});
    expect(loading.busy()).toBe(false);
  });

  it('stops being busy when the call fails', () => {
    http.get('http://api.test/x').subscribe({ error: () => undefined });
    backend.expectOne('http://api.test/x').flush('boom', { status: 500, statusText: 'x' });

    expect(loading.busy()).toBe(false);
  });

  it('stops being busy when the call is cancelled', () => {
    const subscription = http.get('http://api.test/x').subscribe();
    expect(loading.busy()).toBe(true);

    subscription.unsubscribe();
    expect(loading.busy()).toBe(false);
  });

  it('ignores calls that are not to our API (dictionaries, configuration)', () => {
    http.get('i18n/en.json').subscribe();

    expect(loading.busy()).toBe(false);
    backend.expectOne('i18n/en.json').flush({});
  });
});
