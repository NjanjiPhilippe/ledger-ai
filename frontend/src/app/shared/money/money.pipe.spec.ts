import { TestBed } from '@angular/core/testing';
import { translocoTesting } from '../../../testing/transloco';
import { LanguageService } from '../../core/i18n/language.service';
import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  it('formats in the language of the interface, without losing a digit', () => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [translocoTesting()] });
    const language = TestBed.inject(LanguageService);
    const pipe = TestBed.runInInjectionContext(() => new MoneyPipe());

    language.set('en');
    expect(pipe.transform('12345678901234567.89', 'EUR')).toBe('€12,345,678,901,234,567.89');

    language.set('fr');
    expect(pipe.transform('1234.50', 'EUR').replace(/\s/g, ' ')).toBe('1 234,50 €');
  });
});
