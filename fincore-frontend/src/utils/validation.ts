import { onlyDigits } from './format';

const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

function cpfDigit(cpf: string, length: number) {
  let sum = 0;
  for (let i = 0; i < length; i += 1) {
    sum += Number(cpf[i]) * (length + 1 - i);
  }
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

export function isValidCpf(value: string) {
  const cpf = onlyDigits(value);
  if (cpf.length !== 11 || new Set(cpf).size === 1) return false;
  return Number(cpf[9]) === cpfDigit(cpf, 9) && Number(cpf[10]) === cpfDigit(cpf, 10);
}

export const isValidPhone = (value: string) => /^\d{10,11}$/.test(onlyDigits(value));

export const isValidEmail = (value: string) => EMAIL_PATTERN.test(value.trim());

export interface PasswordRule {
  label: string;
  ok: boolean;
}

export function passwordRules(password: string): PasswordRule[] {
  return [
    { label: 'Mínimo de 8 caracteres', ok: password.length >= 8 },
    { label: 'Uma letra maiúscula', ok: /[A-Z]/.test(password) },
    { label: 'Uma letra minúscula', ok: /[a-z]/.test(password) },
    { label: 'Um número', ok: /\d/.test(password) },
    { label: 'Um caractere especial', ok: /[^A-Za-z0-9]/.test(password) },
  ];
}

export const isStrongPassword = (password: string) =>
  passwordRules(password).every(rule => rule.ok);
