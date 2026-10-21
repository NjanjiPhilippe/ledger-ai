import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TranslocoService } from '@jsverse/transloco';
import { TEST_CONFIG } from '../../../testing/app-config';
import { translocoTesting } from '../../../testing/transloco';
import { AuthService } from '../auth/auth.service';
import { AppConfigService } from '../config/app-config';
import { ToastService } from '../toast/toast.service';
import { apiErrorInterceptor, HANDLE_ERRORS_LOCALLY } from './api-error.interceptor';

describe('apiErrorInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let toasts: ToastService;
  const login = vi.fn();

  beforeEach(() => {
    login.mockReset();
    TestBed.configureTestingModule({
      imports: [translocoTesting()],
      providers: [
        provideHttpClient(withInterceptors([apiErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { login } },
        { provide: Router, useValue: { url: '/accounts' } },
      ],
    });
    TestBed.inject(AppConfigService).set(TEST_CONFIG);
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    toasts = TestBed.inject(ToastService);
  });

  afterEach(() => controller.verify());

  /** Performs a GET that fails, and returns the error the caller receives. */
  function fail(url: string, status: number, body: object | null = null, context?: HttpContext) {
    let received: unknown;
    http.get(url, { context }).subscribe({ error: (e) => (received = e) });
    controller.expectOne(url).flush(body, { status, statusText: 'error' });
    return received;
  }

  it('turns a validation error into a translated toast listing the invalid fields', () => {
    fail('http://api.test/api/v1/journal-entries', 400, {
      message: 'Validation failed',
      errors: [{ field: 'description', message: 'Description is required' }],
    });

    expect(toasts.toasts()).toEqual([
      expect.objectContaining({
        level: 'error',
        title: 'Some information is invalid',
        detail: 'description: Description is required',
      }),
    ]);
  });

  it('shows at most three field errors', () => {
    fail('http://api.test/x', 400, {
      errors: ['a', 'b', 'c', 'd'].map((field) => ({ field, message: 'bad' })),
    });

    expect(toasts.toasts()[0].detail).toBe('a: bad · b: bad · c: bad');
  });

  it('adds the backend message for conflicts and unavailable services', () => {
    fail('http://api.test/x', 409, { message: 'Cannot post an entry that is not a draft' });
    fail('http://api.test/y', 502, { message: "Échec de l'appel à Anthropic" });

    expect(toasts.toasts().map((t) => [t.title, t.detail])).toEqual([
      ['This action is not possible right now', 'Cannot post an entry that is not a draft'],
      ['A service we depend on is unavailable', "Échec de l'appel à Anthropic"],
    ]);
  });

  it('never shows the message of a server error, which can carry internals', () => {
    fail('http://api.test/x', 500, { message: 'NullPointerException at com.np3.ledgerai...' });

    expect(toasts.toasts()[0]).toMatchObject({
      title: 'Something went wrong on our side',
      detail: 'Please try again. If the problem continues, contact support.',
    });
  });

  it('says when the server cannot be reached', () => {
    fail('http://api.test/x', 0);

    expect(toasts.toasts()[0]).toMatchObject({
      title: 'Cannot reach the server',
      detail: 'The server did not respond. Check your connection, then try again.',
    });
  });

  it('shows the toast in the active language', () => {
    TestBed.inject(TranslocoService).setActiveLang('fr');

    fail('http://api.test/x', 403);

    expect(toasts.toasts()[0].title).toBe("Vous n'avez pas le droit de faire cela");
  });

  it('sends the user to sign in again when the session has expired', () => {
    fail('http://api.test/x', 401);

    expect(toasts.toasts()[0].title).toBe('Your session has expired');
    expect(login).toHaveBeenCalledWith('/accounts');
  });

  it('rethrows, so the caller can still react to the failure', () => {
    const received = fail('http://api.test/x', 409);

    expect(received).toMatchObject({ status: 409 });
  });

  it('stays silent for a request whose caller handles the error itself', () => {
    fail('http://api.test/x', 400, null, new HttpContext().set(HANDLE_ERRORS_LOCALLY, true));

    expect(toasts.toasts()).toEqual([]);
  });

  it('ignores failures of other hosts', () => {
    fail('http://other.test/x', 500);

    expect(toasts.toasts()).toEqual([]);
  });
});
