import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { translocoTesting } from '../testing/transloco';
import { App } from './app';
import { ToastService } from './core/toast/toast.service';

describe('App', () => {
  it('hosts the router outlet and the toasts, so a message shows on any screen', async () => {
    TestBed.configureTestingModule({
      imports: [App, translocoTesting()],
      providers: [provideRouter([])],
    });
    const fixture = TestBed.createComponent(App);
    TestBed.inject(ToastService).error('Something failed');

    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('[data-testid="toast"]').textContent).toContain(
      'Something failed',
    );
  });
});
