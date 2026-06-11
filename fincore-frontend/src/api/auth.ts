import { authApi } from './axios';
import { AuthResponse } from '../types';

export const login = (email: string, password: string) =>
  authApi.post<AuthResponse>('/api/v1/auth/login', { email, password }).then(r => r.data);

export const register = (
  fullName: string,
  email: string,
  password: string,
  cpf: string
) =>
  authApi
    .post<AuthResponse>('/api/v1/auth/register', { fullName, email, password, cpf })
    .then(r => r.data);

export const getMe = () =>
  authApi.get('/api/v1/auth/me').then(r => r.data);
