import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TEST_CONFIG } from '../../../testing/app-config';
import { AppConfigService } from '../config/app-config';
import { authInterceptor, isApiUrl } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let token: string | null;

  beforeEach(() => {
    token = 'the-access-token';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getAccessToken: () => token } },
      ],
    });
    TestBed.inject(AppConfigService).set(TEST_CONFIG);
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds the bearer token to calls to the API', () => {
    http.get('http://api.test/api/v1/me').subscribe();

    const request = controller.expectOne('http://api.test/api/v1/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer the-access-token');
    request.flush({});
  });

  it.each([
    'http://other.test/api/v1/me',
    'http://api.test.evil.example/api/v1/me',
    'http://api.testing/api/v1/me',
    'i18n/en.json',
  ])('never sends the token to %s', (url) => {
    http.get(url).subscribe();

    const request = controller.expectOne(url);
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('sends no Authorization header when there is no token', () => {
    token = null;

    http.get('http://api.test/api/v1/me').subscribe();

    const request = controller.expectOne('http://api.test/api/v1/me');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});

describe('isApiUrl', () => {
  it('matches the base URL itself and what is below it, not look-alikes', () => {
    expect(isApiUrl('http://api.test', 'http://api.test')).toBe(true);
    expect(isApiUrl('http://api.test/a', 'http://api.test')).toBe(true);
    expect(isApiUrl('http://api.test2/a', 'http://api.test')).toBe(false);
  });
});
