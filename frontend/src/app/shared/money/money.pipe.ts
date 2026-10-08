import { inject, Pipe, PipeTransform } from '@angular/core';
import { LanguageService } from '../../core/i18n/language.service';
import { formatMoney } from './money';

const LOCALES = { en: 'en-US', fr: 'fr-FR' } as const;

/** `amount | money: 'XAF'`: formats a decimal string in the language of the interface (impure: follows a language switch). */
@Pipe({ name: 'money', pure: false })
export class MoneyPipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(amount: string, currencyCode: string): string {
    return formatMoney(amount, currencyCode, LOCALES[this.language.current()]);
  }
}
