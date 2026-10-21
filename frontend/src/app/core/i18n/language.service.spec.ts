import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { translocoTesting } from '../../../testing/transloco';
import { LanguageService } from './language.service';

describe('LanguageService', () => {
  let service: LanguageService;
  let transloco: TranslocoService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [translocoTesting()] });
    service = TestBed.inject(LanguageService);
    transloco = TestBed.inject(TranslocoService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  const browserLanguage = (language: string) =>
    vi.spyOn(navigator, 'language', 'get').mockReturnValue(language);

  it('starts in the language of the browser when it is French', () => {
    browserLanguage('fr-CM');

    service.initialize();

    expect(service.current()).toBe('fr');
    expect(transloco.getActiveLang()).toBe('fr');
    expect(document.documentElement.lang).toBe('fr');
  });

  it('falls back to English for any other browser language', () => {
    browserLanguage('de-DE');

    service.initialize();

    expect(service.current()).toBe('en');
  });

  it('remembers the choice and prefers it to the browser language', () => {
    browserLanguage('fr-FR');
    service.set('en');

    const reopened = TestBed.runInInjectionContext(() => new LanguageService());
    reopened.initialize();

    expect(reopened.current()).toBe('en');
    expect(localStorage.getItem('ledgerai.language')).toBe('en');
  });

  it('ignores a stored value that is not a supported language', () => {
    browserLanguage('en-US');
    localStorage.setItem('ledgerai.language', 'klingon');

    service.initialize();

    expect(service.current()).toBe('en');
  });

  it('still switches language when storage is unavailable', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked');
    });
    browserLanguage('en-US');

    service.initialize();
    expect(() => service.set('fr')).not.toThrow();

    expect(service.current()).toBe('fr');
  });
});
