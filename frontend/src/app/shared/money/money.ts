/**
 * Monetary amounts travel as strings ("1234567890.50"): the backend uses BigDecimal, and JavaScript numbers
 * are binary floating point (0.1 + 0.2 !== 0.3). This module formats them without ever converting to a number,
 * so no digit is lost; calculations on amounts do not belong in the browser.
 */
const DECIMAL_AMOUNT = /^-?\d+(\.\d+)?$/;

export function isDecimalAmount(value: string): boolean {
  return DECIMAL_AMOUNT.test(value);
}

/** Intl.NumberFormat formats a decimal string exactly (ES2023 "string numeric literal" support). */
export function formatMoney(amount: string, currencyCode: string, locale: string): string {
  if (!isDecimalAmount(amount)) {
    throw new Error(`Not a decimal amount: "${amount}"`);
  }
  return new Intl.NumberFormat(locale, { style: 'currency', currency: currencyCode }).format(
    amount as `${number}`,
  );
}
