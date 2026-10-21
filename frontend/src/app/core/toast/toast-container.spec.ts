import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../testing/transloco';
import { ToastContainer } from './toast-container';
import { ToastService } from './toast.service';

describe('ToastContainer', () => {
  async function setup() {
    TestBed.configureTestingModule({ imports: [ToastContainer, translocoTesting()] });
    const fixture = TestBed.createComponent(ToastContainer);
    const toasts = TestBed.inject(ToastService);
    await fixture.whenStable();
    return { fixture, toasts, element: fixture.nativeElement as HTMLElement };
  }

  it('renders nothing when there is no toast', async () => {
    const { element } = await setup();

    expect(element.querySelectorAll('[data-testid="toast"]')).toHaveLength(0);
  });

  it('announces problems as alerts and confirmations as status messages', async () => {
    const { fixture, toasts, element } = await setup();

    toasts.error('Broken', 'because');
    toasts.warning('Careful');
    toasts.success('Saved');
    await fixture.whenStable();

    const rows = Array.from(element.querySelectorAll('[data-testid="toast"]'));
    expect(rows.map((row) => row.getAttribute('role'))).toEqual(['alert', 'alert', 'status']);
    expect(rows[0].textContent).toContain('Broken');
    expect(rows[0].textContent).toContain('because');
  });

  it('lets the user dismiss a toast, with a button named in the active language', async () => {
    const { fixture, toasts, element } = await setup();
    toasts.info('Hello');
    await fixture.whenStable();

    const button = element.querySelector('button') as HTMLButtonElement;
    expect(button.getAttribute('aria-label')).toBe('Dismiss');
    button.click();
    await fixture.whenStable();

    expect(element.querySelectorAll('[data-testid="toast"]')).toHaveLength(0);
  });
});
