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
      class="inline-flex overflow-hidden rounded-lg border border-silver bg-white text-xs font-medium"
      [attr.aria-label]="'language.label' | transloco"
    >
      @for (language of languages; track language) {
        <button
          type="button"
          class="px-3 py-1.5 uppercase"
          [class]="
            languageService.current() === language
              ? 'bg-ink text-white'
              : 'bg-white text-graphite hover:bg-fog'
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
