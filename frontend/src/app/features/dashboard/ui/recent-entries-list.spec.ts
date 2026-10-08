import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../../testing/transloco';
import { ENTRY } from '../../../../testing/ledger';
import { LanguageService } from '../../../core/i18n/language.service';
import { RecentEntriesList } from './recent-entries-list';

describe('RecentEntriesList', () => {
  function render(entries: (typeof ENTRY)[], names: [string, string][]) {
    TestBed.configureTestingModule({ imports: [RecentEntriesList, translocoTesting()] });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(RecentEntriesList);
    fixture.componentRef.setInput('entries', entries);
    fixture.componentRef.setInput('accountNames', new Map(names));
    fixture.componentRef.setInput('unknownAccount', 'Unknown account');
    return fixture.whenStable().then(() => fixture.nativeElement as HTMLElement);
  }

  it('describes an entry with the names of the accounts it touches', async () => {
    const element = await render(
      [ENTRY],
      [
        ['11111111-1111-1111-1111-111111111111', 'Cash'],
        ['33333333-3333-3333-3333-333333333333', 'Sales'],
      ],
    );

    expect(element.querySelector('[data-testid="entry-debited"]')?.textContent).toBe('Cash');
    expect(element.querySelector('[data-testid="entry-credited"]')?.textContent).toBe('Sales');
  });

  it('says "Unknown account" rather than showing an identifier', async () => {
    const element = await render([ENTRY], []);

    expect(element.querySelector('[data-testid="entry-debited"]')?.textContent).toBe(
      'Unknown account',
    );
    expect(element.textContent).not.toContain('1111');
  });

  it('shows an empty state without entries', async () => {
    const element = await render([], []);

    expect(element.querySelector('[data-testid="entries-empty"]')?.textContent).toContain(
      'No journal entry has been recorded yet.',
    );
  });
});
