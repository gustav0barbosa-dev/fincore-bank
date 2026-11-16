export const onlyDigits = (value: string) => value.replace(/\D/g, '');

export const formatCpf = (value: string) =>
  onlyDigits(value)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');

export const formatPhone = (value: string) => {
  const digits = onlyDigits(value).slice(0, 11);
  if (!digits) return '';
  if (digits.length <= 2) return `(${digits}`;
  const area = digits.slice(0, 2);
  const rest = digits.slice(2);
  if (rest.length <= 4) return `(${area}) ${rest}`;
  const split = rest.length > 8 ? 5 : 4;
  return `(${area}) ${rest.slice(0, split)}-${rest.slice(split)}`;
};

export const maskCpf = (value: string) => {
  const digits = onlyDigits(value);
  if (digits.length !== 11) return formatCpf(digits);
  return `${digits.slice(0, 3)}.***.***-${digits.slice(9)}`;
};

export const formatPixKey = (keyType: string, value: string) => {
  if (keyType === 'CPF') return formatCpf(value);
  if (keyType === 'PHONE') return formatPhone(value);
  return value;
};

export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });

export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  });
