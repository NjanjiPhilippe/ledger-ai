import { FULL_TRIAL_BALANCE } from '../../../../testing/ledger';
import { trialBalanceToCsv } from './balance-csv';

const labels = {
  account: 'Account',
  type: 'Type',
  debits: 'Debit',
  credits: 'Credit',
  balance: 'Balance',
  total: 'Total',
  typeOf: (type: string) => type.toLowerCase(),
};

describe('trialBalanceToCsv', () => {
  it('writes a header, every account with its exact amounts, and the totals', () => {
    const lines = trialBalanceToCsv(FULL_TRIAL_BALANCE, labels).trimEnd().split('\r\n');

    expect(lines[0]).toBe('Account,Type,Debit,Credit,Balance');
    expect(lines[1]).toBe('Cash,asset,1950000.00,0.00,1950000.00');
    expect(lines).toHaveLength(7);
    expect(lines.at(-1)).toBe('Total,,2450000.00,2450000.00,');
  });

  it('quotes names that hold a comma, a quote or a line break', () => {
    const report = {
      ...FULL_TRIAL_BALANCE,
      lines: [{ ...FULL_TRIAL_BALANCE.lines[0], accountName: 'Cash, "petty"\nbox' }],
    };

    const csv = trialBalanceToCsv(report, labels);

    expect(csv).toContain('"Cash, ""petty""\nbox",asset');
  });

  it('follows the labels it is given, for the language of the interface', () => {
    const csv = trialBalanceToCsv(FULL_TRIAL_BALANCE, {
      ...labels,
      account: 'Compte',
      debits: 'Débit',
    });

    expect(csv.startsWith('Compte,Type,Débit,')).toBe(true);
  });
});
