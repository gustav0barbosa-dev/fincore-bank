import { transactionApi } from './axios';
import { Page, PixTransaction } from '../types';

export const sendPix = (
  senderAccountId: number,
  receiverKey: string,
  amount: number,
  transactionPin: string,
  description?: string
) =>
  transactionApi
    .post<PixTransaction>('/api/v1/transactions/pix', {
      senderAccountId, receiverKey, amount, transactionPin, description,
    })
    .then(r => r.data);

export const getPixHistory = (accountId: number, page = 0, size = 15) =>
  transactionApi
    .get<Page<PixTransaction>>(`/api/v1/transactions/pix/history/${accountId}`, { params: { page, size } })
    .then(r => r.data);
