import { AccountType } from '../../core/api/api-types';
import { IconName } from './icon';

/** The icon of each account type, the same on every screen. */
export const ACCOUNT_TYPE_ICONS: Record<AccountType, IconName> = {
  ASSET: 'wallet',
  LIABILITY: 'receipt',
  EQUITY: 'pie',
  REVENUE: 'trendingUp',
  EXPENSE: 'trendingDown',
};
