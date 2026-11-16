import { accountApi } from './axios';
import { PixKey, PixKeyType } from '../types';

export const getPixKeys = () =>
  accountApi.get<PixKey[]>('/api/v1/pix-keys').then(r => r.data);

export const createPixKey = (accountId: number, keyType: PixKeyType, keyValue?: string) =>
  accountApi
    .post<PixKey>('/api/v1/pix-keys', {
      accountId,
      keyType,
      keyValue: keyValue?.trim() || undefined,
    })
    .then(r => r.data);

export const deletePixKey = (id: number) =>
  accountApi.delete(`/api/v1/pix-keys/${id}`);
