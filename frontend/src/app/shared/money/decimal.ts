/**
 * Exact decimal arithmetic for what the screen has to compare live (are debits equal to credits?).
 * A value is an integer count of units at a given scale, held in a BigInt: no binary floating point is involved,
 * so 0.1 + 0.2 is exactly 0.3. The backend stays the authority: this only drives the interface.
 */
export interface Decimal {
  readonly units: bigint;
  readonly scale: number;
}

const DECIMAL = /^(-?)(\d+)(?:\.(\d+))?$/;

export const ZERO: Decimal = { units: 0n, scale: 0 };

/** Reads "1234.50" (digits, optional sign and fraction). Anything else is null. */
export function parseDecimal(text: string): Decimal | null {
  const match = DECIMAL.exec(text);
  if (!match) {
    return null;
  }
  const [, sign, whole, fraction = ''] = match;
  const units = BigInt(whole + fraction) * (sign === '-' ? -1n : 1n);
  return { units, scale: fraction.length };
}

function rescale(value: Decimal, scale: number): bigint {
  return value.units * 10n ** BigInt(scale - value.scale);
}

export function add(a: Decimal, b: Decimal): Decimal {
  const scale = Math.max(a.scale, b.scale);
  return { units: rescale(a, scale) + rescale(b, scale), scale };
}

export function subtract(a: Decimal, b: Decimal): Decimal {
  return add(a, { units: -b.units, scale: b.scale });
}

export function sum(values: readonly Decimal[]): Decimal {
  return values.reduce(add, ZERO);
}

export function isZero(value: Decimal): boolean {
  return value.units === 0n;
}

export function isPositive(value: Decimal): boolean {
  return value.units > 0n;
}

export function abs(value: Decimal): Decimal {
  return { units: value.units < 0n ? -value.units : value.units, scale: value.scale };
}

/** The canonical text of a value, which formatMoney and the API both accept: "1500000" or "12.50". */
export function toDecimalString(value: Decimal): string {
  const negative = value.units < 0n;
  const digits = (negative ? -value.units : value.units).toString().padStart(value.scale + 1, '0');
  const whole = digits.slice(0, digits.length - value.scale);
  const fraction = digits.slice(digits.length - value.scale);
  return `${negative ? '-' : ''}${whole}${value.scale > 0 ? '.' + fraction : ''}`;
}

/**
 * What people type into an amount field: spaces (thousands separators) are dropped and a decimal comma becomes
 * a dot. The result still has to pass parseDecimal.
 */
export function normalizeTypedAmount(text: string): string {
  return text.replace(/[\s  ]/g, '').replace(',', '.');
}
