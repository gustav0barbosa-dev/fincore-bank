import { transactionApi } from './axios';
import { Page, PixTransaction } from '../types';

export const sendPix = (
  senderAccountId: number,
  receiverKey: string,
  amount: number,
  description?: string
) =>
  transactionApi
    .post<PixTransaction>('/api/v1/pix', { senderAccountId, receiverKey, amount, description })
    .then(r => r.data);

export const getPixHistory = (accountId: number, page = 0, size = 15) =>
  transactionApi
    .get<Page<PixTransaction>>('/api/v1/pix/history', { params: { accountId, page, size } })
    .then(r => r.data);
