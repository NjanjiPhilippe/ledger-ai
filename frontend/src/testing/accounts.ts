import { AccountResponse, PagedAccounts } from '../app/core/api/api-types';

export const CASH: AccountResponse = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Cash',
  type: 'ASSET',
  currencyCode: 'XAF',
  active: true,
};

export const OLD_SUSPENSE: AccountResponse = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Old suspense',
  type: 'LIABILITY',
  currencyCode: 'EUR',
  active: false,
};

export function pageOf(
  content: AccountResponse[],
  options: Partial<PagedAccounts> = {},
): PagedAccounts {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    ...options,
  };
}
