import { inject, Injectable, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';

export const LANGUAGES = ['en', 'fr'] as const;
export type Language = (typeof LANGUAGES)[number];

const STORAGE_KEY = 'ledgerai.language';

/** The interface language: remembered across visits, otherwise the browser's, otherwise English. */
@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);
  private readonly selected = signal<Language>('en');

  readonly current = this.selected.asReadonly();

  /**
   * Applies the language and waits for its dictionary: an error toast raised during start-up would otherwise
   * show raw keys such as "errors.network.title".
   */
  async initialize(): Promise<void> {
    const language = this.stored() ?? this.fromBrowser();
    this.apply(language);
    try {
      await firstValueFrom(this.transloco.load(language));
    } catch {
      // Dictionary unreachable: start anyway, the keys show instead of the app failing to open.
    }
  }

  set(language: Language): void {
    this.apply(language);
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      // Storage can be unavailable (private mode, blocked site data): the choice then lasts for the visit only.
    }
  }

  private apply(language: Language): void {
    this.selected.set(language);
    this.transloco.setActiveLang(language);
    document.documentElement.lang = language;
  }

  private stored(): Language | null {
    try {
      const value = localStorage.getItem(STORAGE_KEY);
      return LANGUAGES.find((l) => l === value) ?? null;
    } catch {
      return null;
    }
  }

  private fromBrowser(): Language {
    return navigator.language.toLowerCase().startsWith('fr') ? 'fr' : 'en';
  }
}
