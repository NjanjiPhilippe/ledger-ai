import { AccountType } from '../../../core/api/api-types';
import { IconName } from '../../../shared/ui/icon';

export const ACCOUNT_TYPE_ICONS: Record<AccountType, IconName> = {
  ASSET: 'wallet',
  LIABILITY: 'receipt',
  EQUITY: 'pie',
  REVENUE: 'trendingUp',
  EXPENSE: 'trendingDown',
};
