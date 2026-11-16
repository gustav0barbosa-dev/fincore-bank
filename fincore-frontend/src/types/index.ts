export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  email: string;
  fullName: string;
  role?: string;
}

export interface User {
  email: string;
  fullName: string;
  role: string;
}

export interface Profile {
  id: number;
  fullName: string;
  email: string;
  cpf: string | null;
  phone: string | null;
  role: string;
  enabled: boolean;
  transactionPinConfigured: boolean;
  createdAt: string;
}

export type PixKeyType = 'CPF' | 'EMAIL' | 'PHONE' | 'RANDOM';

export interface PixKey {
  id: number;
  keyType: PixKeyType;
  keyValue: string;
  accountId: number;
  accountNumber: string;
  ownerEmail: string;
  createdAt: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  agency: string;
  ownerEmail: string;
  ownerName: string;
  balance: number;
  accountType: 'CHECKING' | 'SAVINGS';
  status: 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
  createdAt: string;
}

export interface Transaction {
  id: number;
  transactionId: string;
  type: 'PIX_IN' | 'PIX_OUT' | 'TED_IN' | 'TED_OUT' | 'DEPOSIT' | 'WITHDRAWAL';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string | null;
  counterpartKey: string | null;
  status: string;
  createdAt: string;
}

export interface PixTransaction {
  id: number;
  transactionId: string;
  senderAccountId: number;
  senderEmail: string;
  receiverKey: string;
  receiverAccountId: number | null;
  amount: number;
  description: string | null;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  failureReason: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
