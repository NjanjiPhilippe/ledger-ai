import { inject, Pipe, PipeTransform } from '@angular/core';
import { LanguageService } from '../../core/i18n/language.service';

const LOCALES = { en: 'en-US', fr: 'fr-FR' } as const;

/** `iso | shortDate` gives "Oct 8, 2026" / "8 oct. 2026"; `iso | shortDate: 'long'` adds the weekday. */
@Pipe({ name: 'shortDate', pure: false })
export class ShortDatePipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(value: string | Date | null | undefined, style: 'short' | 'long' = 'short'): string {
    if (value === null || value === undefined) {
      return '';
    }
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    const options: Intl.DateTimeFormatOptions =
      style === 'long'
        ? { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }
        : { year: 'numeric', month: 'short', day: 'numeric' };
    return new Intl.DateTimeFormat(LOCALES[this.language.current()], options).format(date);
  }
}
