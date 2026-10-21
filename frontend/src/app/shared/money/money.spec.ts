import { formatMoney, isDecimalAmount } from './money';

describe('formatMoney', () => {
  it('formats a decimal string with the conventions of the locale', () => {
    expect(formatMoney('1234.50', 'EUR', 'en')).toBe('€1,234.50');
    expect(formatMoney('1234.50', 'EUR', 'fr-FR').replace(/\s/g, ' ')).toBe('1 234,50 €');
  });

  it('formats a currency without decimals', () => {
    expect(formatMoney('100000', 'XAF', 'fr-FR')).toContain('100');
    expect(formatMoney('100000', 'XAF', 'en')).toContain('100,000');
  });

  it('keeps every digit of an amount a floating point number cannot hold', () => {
    // 1234567890123456.78 is not representable as a double: Number() would turn it into ...456.75.
    expect(Number('1234567890123456.78').toFixed(2)).not.toBe('1234567890123456.78');
    expect(formatMoney('1234567890123456.78', 'EUR', 'en')).toBe('€1,234,567,890,123,456.78');
  });

  it('formats negative amounts', () => {
    expect(formatMoney('-50.25', 'EUR', 'en')).toBe('-€50.25');
  });

  it.each(['', 'abc', '1,5', '1e3', ' 10', '1.', '.5', 'NaN'])('rejects "%s"', (value) => {
    expect(() => formatMoney(value, 'EUR', 'en')).toThrow(/Not a decimal amount/);
  });
});

describe('isDecimalAmount', () => {
  it('accepts what the API sends', () => {
    expect(['0', '100', '100.5', '-0.01', '1234567890.50'].every(isDecimalAmount)).toBe(true);
  });
});
