import {
  abs,
  add,
  isPositive,
  isZero,
  normalizeTypedAmount,
  parseDecimal,
  subtract,
  sum,
  toDecimalString,
  Decimal,
} from './decimal';

const d = (text: string): Decimal => {
  const value = parseDecimal(text);
  if (!value) {
    throw new Error(`not a decimal: ${text}`);
  }
  return value;
};

describe('decimal', () => {
  it('reads digits with an optional sign and fraction, and nothing else', () => {
    expect(parseDecimal('1500000')).toEqual({ units: 1500000n, scale: 0 });
    expect(parseDecimal('-12.50')).toEqual({ units: -1250n, scale: 2 });
    for (const bad of ['', '1,5', '1.', '.5', 'abc', '1e3', '1 000', '--1']) {
      expect(parseDecimal(bad)).toBeNull();
    }
  });

  it('adds without the binary floating point error', () => {
    expect(0.1 + 0.2).not.toBe(0.3);
    expect(toDecimalString(add(d('0.1'), d('0.2')))).toBe('0.3');
  });

  it('keeps every digit of a very large amount', () => {
    expect(toDecimalString(add(d('12345678901234567.89'), d('0.11')))).toBe('12345678901234568.00');
  });

  it('adds values of different scales', () => {
    expect(toDecimalString(add(d('100'), d('0.005')))).toBe('100.005');
    expect(toDecimalString(sum([d('1.5'), d('2.25'), d('3')]))).toBe('6.75');
    expect(toDecimalString(sum([]))).toBe('0');
  });

  it('subtracts and takes the absolute value', () => {
    const gap = subtract(d('1788750'), d('1500000'));
    expect(toDecimalString(gap)).toBe('288750');
    expect(toDecimalString(abs(subtract(d('1'), d('2.5'))))).toBe('1.5');
    expect(isZero(subtract(d('2.50'), d('2.5')))).toBe(true);
  });

  it('tells positive, zero and negative apart', () => {
    expect(isPositive(d('0.01'))).toBe(true);
    expect(isPositive(d('0'))).toBe(false);
    expect(isPositive(d('-1'))).toBe(false);
  });

  it('writes small and negative values properly', () => {
    expect(toDecimalString({ units: 5n, scale: 2 })).toBe('0.05');
    expect(toDecimalString({ units: -5n, scale: 2 })).toBe('-0.05');
  });

  it('cleans what people type: thousands separators and decimal comma', () => {
    expect(normalizeTypedAmount('1 500 000')).toBe('1500000');
    expect(normalizeTypedAmount('1 788,75')).toBe('1788.75');
    expect(normalizeTypedAmount(' 12.5 ')).toBe('12.5');
  });
});
