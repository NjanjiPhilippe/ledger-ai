import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LANGUAGES, Language, LanguageService } from './language.service';

@Component({
  selector: 'app-language-switcher',
  imports: [TranslocoPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      role="group"
      class="inline-flex overflow-hidden rounded border border-slate-300 text-xs"
      [attr.aria-label]="'language.label' | transloco"
    >
      @for (language of languages; track language) {
        <button
          type="button"
          class="px-2 py-1 uppercase"
          [class]="
            languageService.current() === language
              ? 'bg-slate-800 text-white'
              : 'bg-white text-slate-700 hover:bg-slate-100'
          "
          [attr.aria-pressed]="languageService.current() === language"
          [attr.aria-label]="'language.' + language | transloco"
          [attr.lang]="language"
          (click)="select(language)"
        >
          {{ language }}
        </button>
      }
    </div>
  `,
})
export class LanguageSwitcher {
  protected readonly languageService = inject(LanguageService);
  protected readonly languages = LANGUAGES;

  protected select(language: Language): void {
    this.languageService.set(language);
  }
}
