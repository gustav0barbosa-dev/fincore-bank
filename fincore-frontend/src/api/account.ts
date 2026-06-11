import { accountApi } from './axios';
import { Account, Page, Transaction } from '../types';

export const getMyAccounts = () =>
  accountApi.get<Account[]>('/api/v1/accounts/my').then(r => r.data);

export const createAccount = (
  ownerName: string,
  ownerEmail: string,
  accountType: 'CHECKING' | 'SAVINGS' = 'CHECKING'
) =>
  accountApi
    .post<Account>('/api/v1/accounts', { ownerName, ownerEmail, accountType })
    .then(r => r.data);

export const deposit = (accountId: number, amount: number, description: string) =>
  accountApi
    .post<Account>(`/api/v1/accounts/${accountId}/deposit`, { amount, description })
    .then(r => r.data);

export const getStatement = (accountId: number, page = 0, size = 15) =>
  accountApi
    .get<Page<Transaction>>(`/api/v1/accounts/${accountId}/statement`, {
      params: { page, size },
    })
    .then(r => r.data);
