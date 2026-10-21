import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../testing/transloco';
import { LanguageService } from '../../core/i18n/language.service';
import { ShortDatePipe } from './short-date.pipe';

describe('ShortDatePipe', () => {
  function create() {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [translocoTesting()] });
    return {
      language: TestBed.inject(LanguageService),
      pipe: TestBed.runInInjectionContext(() => new ShortDatePipe()),
    };
  }

  it('formats in the language of the interface', () => {
    const { language, pipe } = create();
    const date = new Date(2026, 9, 8, 12);

    language.set('en');
    expect(pipe.transform(date)).toBe('Oct 8, 2026');
    expect(pipe.transform(date, 'long')).toBe('Thursday, October 8, 2026');

    language.set('fr');
    expect(pipe.transform(date, 'long')).toBe('jeudi 8 octobre 2026');
  });

  it('gives nothing for a missing or invalid date', () => {
    const { pipe } = create();

    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform('not a date')).toBe('');
  });
});
