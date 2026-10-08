import { computed, inject, Injectable, signal } from '@angular/core';
import { Advice, AdviceCategory, Recommendation, Severity } from '../../../core/api/api-types';
import { AdvisorApi } from '../data-access/advisor.api';

export type AdvisorStatus = 'idle' | 'analyzing' | 'done' | 'error';

export interface AdviceItem {
  readonly severity: Severity;
  readonly category: AdviceCategory;
  readonly title: string;
  readonly detail: string;
}

const SEVERITY_RANK: Record<Severity, number> = { CRITICAL: 0, WARNING: 1, INFO: 2 };
const CATEGORIES: readonly AdviceCategory[] = [
  'LIQUIDITY',
  'PROFITABILITY',
  'RISK',
  'COMPLIANCE',
  'GROWTH',
  'GENERAL',
];

function severityOf(value: string): Severity {
  return value === 'CRITICAL' || value === 'WARNING' ? value : 'INFO';
}

function categoryOf(value: string): AdviceCategory {
  return CATEGORIES.find((c) => c === value) ?? 'GENERAL';
}

function toItem(recommendation: Recommendation): AdviceItem {
  return {
    severity: severityOf(recommendation.severity),
    category: categoryOf(recommendation.category),
    title: recommendation.title,
    detail: recommendation.detail,
  };
}

/**
 * The advisor's last analysis. It lives for the whole session, not for the screen: an analysis costs time and money,
 * so coming back to the page shows the last one instead of running another. Nothing is stored on the server.
 */
@Injectable({ providedIn: 'root' })
export class AdvisorStore {
  private readonly api = inject(AdvisorApi);

  readonly status = signal<AdvisorStatus>('idle');
  readonly advice = signal<Advice | null>(null);
  /** 'ALL', or the category the person is looking at. */
  readonly category = signal<AdviceCategory | 'ALL'>('ALL');

  /** Most serious first; the advisor's own order is kept within a severity. */
  readonly items = computed<AdviceItem[]>(() =>
    (this.advice()?.recommendations ?? [])
      .map(toItem)
      .map((item, index) => ({ item, index }))
      .sort(
        (a, b) =>
          SEVERITY_RANK[a.item.severity] - SEVERITY_RANK[b.item.severity] || a.index - b.index,
      )
      .map(({ item }) => item),
  );

  readonly visibleItems = computed(() =>
    this.category() === 'ALL'
      ? this.items()
      : this.items().filter((i) => i.category === this.category()),
  );

  readonly severityCounts = computed(() => {
    const counts: Record<Severity, number> = { CRITICAL: 0, WARNING: 0, INFO: 0 };
    this.items().forEach((i) => counts[i.severity]++);
    return counts;
  });

  /** Only the categories the advisor actually used, in a fixed order, with how many items each has. */
  readonly categories = computed(() =>
    CATEGORIES.map((category) => ({
      category,
      count: this.items().filter((i) => i.category === category).length,
    })).filter((c) => c.count > 0),
  );

  analyze(): void {
    if (this.status() === 'analyzing') {
      return;
    }
    const previous = this.advice();
    this.status.set('analyzing');
    this.api.analyzeLedger().subscribe({
      next: (advice) => {
        this.advice.set(advice);
        this.category.set('ALL');
        this.status.set('done');
      },
      // Already toasted by the interceptor. A failed attempt does not erase the last good analysis.
      error: () => this.status.set(previous ? 'done' : 'error'),
    });
  }
}
