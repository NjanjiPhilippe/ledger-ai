import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../testing/transloco';
import { ConfirmDialog } from './confirm-dialog';

@Component({
  imports: [ConfirmDialog],
  template: `
    <app-confirm-dialog
      title="Reverse?"
      message="This cannot be undone."
      confirmLabel="Reverse"
      [danger]="true"
      (confirmed)="confirmed = confirmed + 1"
      (dismissed)="dismissed = dismissed + 1"
    />
  `,
})
class Host {
  confirmed = 0;
  dismissed = 0;
}

describe('ConfirmDialog', () => {
  async function open() {
    TestBed.configureTestingModule({ imports: [Host, translocoTesting()] });
    const fixture = TestBed.createComponent(Host);
    document.body.appendChild(fixture.nativeElement);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const button = (element: HTMLElement, id: string) =>
    element.querySelector(`[data-testid="${id}"]`) as HTMLButtonElement;

  it('asks the question and starts on the safe answer, not on the one that cannot be undone', async () => {
    const { element } = await open();

    expect(element.querySelector('[role="alertdialog"]')?.textContent).toContain(
      'This cannot be undone.',
    );
    expect(document.activeElement).toBe(button(element, 'confirm-cancel'));
  });

  it('confirms or dismisses, and dismisses on Escape', async () => {
    const { fixture, element } = await open();

    button(element, 'confirm-ok').click();
    button(element, 'confirm-cancel').click();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(fixture.componentInstance.confirmed).toBe(1);
    expect(fixture.componentInstance.dismissed).toBe(2);
  });
});
