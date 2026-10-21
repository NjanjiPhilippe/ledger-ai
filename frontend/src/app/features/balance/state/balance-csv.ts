import { TrialBalance } from '../../../core/api/api-types';

/** One CSV field: quoted when it holds a separator, a quote or a line break, with quotes doubled (RFC 4180). */
function field(value: string): string {
  return /[",;\r\n]/.test(value) ? `"${value.replace(/"/g, '""')}"` : value;
}

/**
 * The trial balance as CSV, with the exact amounts of the API (no rounding, no currency symbol, a dot for the
 * decimal): what a spreadsheet or an accountant's tool can read back without loss. Labels are given by the caller
 * so that the file follows the language of the interface.
 */
export function trialBalanceToCsv(
  report: TrialBalance,
  labels: {
    account: string;
    type: string;
    debits: string;
    credits: string;
    balance: string;
    total: string;
    typeOf: (type: string) => string;
  },
): string {
  const header = [labels.account, labels.type, labels.debits, labels.credits, labels.balance];
  const rows = report.lines.map((line) => [
    line.accountName,
    labels.typeOf(line.accountType),
    line.totalDebits,
    line.totalCredits,
    line.balance,
  ]);
  const total = [labels.total, '', report.totalDebits, report.totalCredits, ''];
  return [header, ...rows, total].map((row) => row.map(field).join(',')).join('\r\n') + '\r\n';
}
