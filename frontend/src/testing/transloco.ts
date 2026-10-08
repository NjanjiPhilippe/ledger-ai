import { TranslocoTestingModule } from '@jsverse/transloco';
import en from '../../public/i18n/en.json';
import fr from '../../public/i18n/fr.json';

/** The real dictionaries, so tests assert the text users actually see (and can check both languages). */
export function translocoTesting(defaultLang: 'en' | 'fr' = 'en') {
  return TranslocoTestingModule.forRoot({
    langs: { en, fr },
    translocoConfig: {
      availableLangs: ['en', 'fr'],
      defaultLang,
      fallbackLang: 'en',
      reRenderOnLangChange: true, // same as the application: the text must follow a language switch
    },
    preloadLangs: true,
  });
}
