import { authApi } from './axios';
import { AuthResponse, Profile } from '../types';

export const login = (email: string, password: string) =>
  authApi.post<AuthResponse>('/api/v1/auth/login', { email, password }).then(r => r.data);

export const register = (
  fullName: string,
  email: string,
  password: string,
  cpf: string,
  transactionPin: string,
  phone?: string
) =>
  authApi
    .post<AuthResponse>('/api/v1/auth/register', {
      fullName, email, password, cpf, transactionPin, phone: phone || undefined,
    })
    .then(r => r.data);

export const getProfile = () =>
  authApi.get<Profile>('/api/v1/auth/me').then(r => r.data);

/** @deprecated use getProfile */
export const getMe = getProfile;

export const updateProfile = (fullName: string, phone?: string) =>
  authApi
    .put<Profile>('/api/v1/auth/profile', { fullName, phone: phone || undefined })
    .then(r => r.data);

export const changePassword = (currentPassword: string, newPassword: string) =>
  authApi.put('/api/v1/auth/password', { currentPassword, newPassword });

export const changeTransactionPin = (currentPassword: string, newPin: string) =>
  authApi.put('/api/v1/auth/transaction-pin', { currentPassword, newPin });
