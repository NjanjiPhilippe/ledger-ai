import en from '../../../../public/i18n/en.json';
import fr from '../../../../public/i18n/fr.json';

type Dictionary = { [key: string]: string | Dictionary };

function flatten(dictionary: Dictionary, prefix = ''): Record<string, string> {
  return Object.entries(dictionary).reduce<Record<string, string>>((flat, [key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return typeof value === 'string'
      ? { ...flat, [path]: value }
      : { ...flat, ...flatten(value, path) };
  }, {});
}

const placeholders = (text: string) => (text.match(/{{\s*\w+\s*}}/g) ?? []).sort();

describe('dictionaries', () => {
  const english = flatten(en);
  const french = flatten(fr);

  it('define exactly the same keys in English and French', () => {
    expect(Object.keys(french).sort()).toEqual(Object.keys(english).sort());
  });

  it('have no empty text', () => {
    for (const [key, text] of [...Object.entries(english), ...Object.entries(french)]) {
      expect(text.trim(), key).not.toBe('');
    }
  });

  it('use the same interpolation placeholders in both languages', () => {
    for (const key of Object.keys(english)) {
      expect(placeholders(french[key]), key).toEqual(placeholders(english[key]));
    }
  });

  it('cover every kind of problem the error interceptor can report', () => {
    const kinds = [
      'network',
      'unauthorized',
      'forbidden',
      'notFound',
      'validation',
      'conflict',
      'gateway',
      'server',
      'unknown',
    ];
    for (const kind of kinds) {
      expect(english[`errors.${kind}.title`], kind).toBeDefined();
    }
  });
});
