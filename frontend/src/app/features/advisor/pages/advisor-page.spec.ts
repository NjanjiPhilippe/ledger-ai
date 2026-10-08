import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ADVICE } from '../../../../testing/ledger';
import { translocoTesting } from '../../../../testing/transloco';
import { LanguageService } from '../../../core/i18n/language.service';
import { AdvisorApi } from '../data-access/advisor.api';
import { AdvisorPage } from './advisor-page';

describe('AdvisorPage', () => {
  const api = { analyzeLedger: vi.fn() };

  async function open() {
    localStorage.clear();
    api.analyzeLedger.mockReset().mockReturnValue(of(ADVICE));
    TestBed.configureTestingModule({
      imports: [AdvisorPage, translocoTesting()],
      providers: [{ provide: AdvisorApi, useValue: api }],
    });
    TestBed.inject(LanguageService).initialize();
    const fixture = TestBed.createComponent(AdvisorPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  const q = (element: HTMLElement, id: string) =>
    element.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const all = (element: HTMLElement, id: string) =>
    Array.from(element.querySelectorAll(`[data-testid="${id}"]`)) as HTMLElement[];
  const text = (element: HTMLElement, id: string) =>
    q(element, id)?.textContent?.replace(/\s+/g, ' ').trim();

  async function analyze(fixture: { whenStable: () => Promise<unknown> }, element: HTMLElement) {
    q(element, 'analyze')?.click();
    await fixture.whenStable();
  }

  it('asks for nothing on its own, and explains what an analysis does with the data', async () => {
    const { element } = await open();

    expect(api.analyzeLedger).not.toHaveBeenCalled();
    expect(text(element, 'idle')).toContain('No analysis yet');
    expect(text(element, 'idle')).toContain('sent to the configured AI provider');
    expect(text(element, 'analyze')).toBe('Analyze the ledger');
  });

  it('shows the recommendations most serious first, with severity and category', async () => {
    const { fixture, element } = await open();

    await analyze(fixture, element);

    expect(all(element, 'title').map((e) => e.textContent?.trim())).toEqual([
      'Expenses exceed revenue',
      'Low cash cover',
      'Unusual rent',
      'Reinvest idle cash',
    ]);
    expect(all(element, 'severity').map((e) => e.textContent?.trim())).toEqual([
      'Critical',
      'Warning',
      'Warning',
      'For information',
    ]);
    expect(all(element, 'category')[0].textContent?.trim()).toBe('Risk');
    expect(text(element, 'counts')).toBe('Critical: 1 Warning: 2 Info: 1');
    expect(text(element, 'meta')).toContain('by anthropic');
    expect(text(element, 'analyze')).toBe('Analyze again');
    expect(text(element, 'disclaimer')).toContain('not accounting or legal advice');
  });

  it('filters by topic, with the count of each, and shows everything again', async () => {
    const { fixture, element } = await open();
    await analyze(fixture, element);

    expect(all(element, 'filter-category').map((b) => b.textContent?.trim())).toEqual([
      'Liquidity (1)',
      'Risk (2)',
      'Growth (1)',
    ]);
    all(element, 'filter-category')[1].click();
    await fixture.whenStable();
    expect(all(element, 'recommendation')).toHaveLength(2);

    q(element, 'filter-all')?.click();
    await fixture.whenStable();
    expect(all(element, 'recommendation')).toHaveLength(4);
  });

  it('keeps line breaks of a detail', async () => {
    const { fixture, element } = await open();
    await analyze(fixture, element);

    expect(all(element, 'detail')[1].textContent).toContain('\n');
  });

  it('says when the advisor has nothing to report', async () => {
    const { fixture, element } = await open();
    api.analyzeLedger.mockReturnValue(of({ ...ADVICE, recommendations: [] }));

    await analyze(fixture, element);

    expect(q(element, 'nothing')).not.toBeNull();
    expect(q(element, 'recommendation')).toBeNull();
  });

  it('tells the analysis failed, and lets the person try again', async () => {
    const { fixture, element } = await open();
    api.analyzeLedger.mockReturnValueOnce(throwError(() => new Error('502')));

    await analyze(fixture, element);
    expect(q(element, 'error')).not.toBeNull();
    expect((q(element, 'analyze') as HTMLButtonElement).disabled).toBe(false);

    await analyze(fixture, element);
    expect(q(element, 'error')).toBeNull();
    expect(all(element, 'recommendation')).toHaveLength(4);
  });

  it('is in French when the language is French', async () => {
    const { fixture, element } = await open();
    await analyze(fixture, element);

    TestBed.inject(LanguageService).set('fr');
    await fixture.whenStable();

    expect(all(element, 'severity')[0].textContent?.trim()).toBe('Critique');
    expect(all(element, 'category')[0].textContent?.trim()).toBe('Risque');
  });
});
