import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../testing/transloco';
import { Panel, PanelStatus } from './panel';

@Component({
  imports: [Panel],
  template: `
    <app-panel title="Things" icon="book" [status]="status()" (retry)="retried = retried + 1">
      <p data-testid="content">Hello</p>
    </app-panel>
  `,
})
class Host {
  readonly status = signal<PanelStatus>('loading');
  retried = 0;
}

describe('Panel', () => {
  async function render() {
    TestBed.configureTestingModule({ imports: [Host, translocoTesting()] });
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  it('shows a loading placeholder, not the content, while loading', async () => {
    const { element } = await render();

    expect(element.querySelector('[data-testid="panel-loading"]')).not.toBeNull();
    expect(element.querySelector('[data-testid="content"]')).toBeNull();
  });

  it('shows the content once loaded', async () => {
    const { fixture, element } = await render();

    fixture.componentInstance.status.set('loaded');
    await fixture.whenStable();

    expect(element.querySelector('[data-testid="content"]')?.textContent).toBe('Hello');
    expect(element.querySelector('h2')?.textContent).toBe('Things');
  });

  it('explains the failure and emits retry', async () => {
    const { fixture, element } = await render();

    fixture.componentInstance.status.set('error');
    await fixture.whenStable();
    (element.querySelector('[data-testid="panel-error"] button') as HTMLButtonElement).click();

    expect(element.querySelector('[data-testid="panel-error"]')?.textContent).toContain(
      'This section could not be loaded.',
    );
    expect(fixture.componentInstance.retried).toBe(1);
  });
});
