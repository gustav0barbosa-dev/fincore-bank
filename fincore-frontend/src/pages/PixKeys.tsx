import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AtSign, Check, Copy, KeyRound, Phone, Plus, Shuffle, Trash2, User, X,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { getMyAccounts } from '../api/account';
import { createPixKey, deletePixKey, getPixKeys } from '../api/pixKeys';
import { Skeleton } from '../components/Skeleton';
import { PixKey, PixKeyType } from '../types';
import { formatCpf, formatDate, formatPhone, formatPixKey } from '../utils/format';
import { isValidCpf, isValidEmail, isValidPhone } from '../utils/validation';
import { apiErrorMessage } from '../utils/errors';

const KEY_TYPES: { value: PixKeyType; label: string; icon: React.ElementType }[] = [
  { value: 'CPF',    label: 'CPF',       icon: User   },
  { value: 'EMAIL',  label: 'E-mail',    icon: AtSign },
  { value: 'PHONE',  label: 'Telefone',  icon: Phone  },
  { value: 'RANDOM', label: 'Aleatória', icon: Shuffle },
];

const TYPE_LABEL: Record<PixKeyType, string> = {
  CPF: 'CPF',
  EMAIL: 'E-mail',
  PHONE: 'Telefone',
  RANDOM: 'Chave aleatória',
};

const inputCls =
  'w-full bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-3.5 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-indigo-500 transition-colors';

function KeyIcon({ type }: { type: PixKeyType }) {
  const Icon = KEY_TYPES.find(t => t.value === type)?.icon ?? KeyRound;
  return <Icon size={16} className="text-indigo-400" />;
}

export default function PixKeys() {
  const [keyType, setKeyType] = useState<PixKeyType>('EMAIL');
  const [keyValue, setKeyValue] = useState('');
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const qc = useQueryClient();

  const { data: accounts, isLoading: loadingAccounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: getMyAccounts,
  });
  const account = accounts?.[0];

  const { data: keys, isLoading: loadingKeys } = useQuery({
    queryKey: ['pix-keys'],
    queryFn: getPixKeys,
  });

  const createMutation = useMutation({
    mutationFn: (value?: string) => createPixKey(account!.id, keyType, value),
    onSuccess: () => {
      toast.success('Chave PIX cadastrada!');
      setKeyValue('');
      qc.invalidateQueries({ queryKey: ['pix-keys'] });
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Erro ao cadastrar chave PIX')),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => deletePixKey(id),
    onSuccess: () => {
      toast.success('Chave PIX removida');
      setConfirmId(null);
      qc.invalidateQueries({ queryKey: ['pix-keys'] });
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Erro ao remover chave PIX')),
  });

  const handleTypeChange = (type: PixKeyType) => {
    setKeyType(type);
    setKeyValue('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!account) return toast.error('Crie uma conta bancária antes de cadastrar chaves');

    let value: string | undefined = keyValue.trim();
    if (keyType === 'RANDOM' && !value) {
      value = undefined;
    } else if (!value) {
      return toast.error('Informe o valor da chave PIX');
    }

    if (keyType === 'CPF' && !isValidCpf(value!)) return toast.error('CPF inválido');
    if (keyType === 'EMAIL' && !isValidEmail(value!)) return toast.error('E-mail inválido');
    if (keyType === 'PHONE' && !isValidPhone(value!)) return toast.error('Telefone deve ter 10 ou 11 dígitos');
    if (keyType === 'RANDOM' && value && !/^[0-9a-f-]{36}$/i.test(value)) {
      return toast.error('Chave aleatória inválida');
    }

    createMutation.mutate(value);
  };

  const handleChange = (value: string) => {
    if (keyType === 'CPF') return setKeyValue(formatCpf(value));
    if (keyType === 'PHONE') return setKeyValue(formatPhone(value));
    setKeyValue(value);
  };

  const copyKey = async (key: PixKey) => {
    try {
      await navigator.clipboard.writeText(key.keyValue);
      toast.success('Chave copiada!');
    } catch {
      toast.error('Não foi possível copiar a chave');
    }
  };

  return (
    <div className="space-y-6 max-w-xl animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Minhas chaves PIX</h1>
        <p className="text-gray-500 text-sm mt-0.5">Cadastre chaves para receber transferências</p>
      </div>

      {!loadingAccounts && !account ? (
        <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-8 text-center">
          <div className="w-14 h-14 rounded-2xl bg-indigo-500/10 flex items-center justify-center mx-auto mb-4">
            <KeyRound size={24} className="text-indigo-400" />
          </div>
          <h2 className="text-white font-semibold mb-1">Você ainda não tem uma conta</h2>
          <p className="text-gray-500 text-sm mb-5">Crie sua conta para poder cadastrar chaves PIX.</p>
          <Link
            to="/"
            className="inline-flex items-center gap-2 bg-indigo-500 hover:bg-indigo-600 text-white text-sm font-semibold px-5 py-2.5 rounded-xl transition-colors"
          >
            Ir para o dashboard
          </Link>
        </div>
      ) : (
        <>
          {account && (
            <div className="flex justify-between items-center bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-4 py-2.5 text-sm">
              <span className="text-gray-500">Chaves vinculadas à conta</span>
              <span className="text-indigo-400 font-semibold">
                Ag. {account.agency} · {account.accountNumber}
              </span>
            </div>
          )}

          {/* Create form */}
          <form onSubmit={handleSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
            <div>
              <label className="text-gray-400 text-xs font-medium block mb-2 uppercase tracking-wide">
                Tipo de chave
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {KEY_TYPES.map(({ value, label, icon: Icon }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => handleTypeChange(value)}
                    className={`flex items-center justify-center gap-1.5 text-xs font-medium px-2 py-2 rounded-xl border transition-colors ${
                      keyType === value
                        ? 'border-indigo-500 bg-indigo-500/15 text-indigo-300'
                        : 'border-[#1e1e2e] text-gray-400 hover:text-gray-200 hover:border-indigo-500/40'
                    }`}
                  >
                    <Icon size={13} /> {label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
                {keyType === 'RANDOM' ? 'Chave aleatória' : `Valor da chave (${TYPE_LABEL[keyType]})`}
              </label>
              {keyType === 'RANDOM' ? (
                <>
                  <input
                    value={keyValue}
                    onChange={e => setKeyValue(e.target.value)}
                    placeholder="Deixe em branco para gerar automaticamente"
                    className={inputCls}
                  />
                  <p className="text-gray-600 text-xs mt-1.5">
                    Se não informar um UUID, o FinCore gera uma chave segura para você.
                  </p>
                </>
              ) : (
                <input
                  value={keyValue}
                  onChange={e => handleChange(e.target.value)}
                  required
                  type={keyType === 'EMAIL' ? 'email' : 'text'}
                  inputMode={keyType === 'CPF' || keyType === 'PHONE' ? 'numeric' : 'text'}
                  placeholder={
                    keyType === 'CPF' ? '000.000.000-00'
                      : keyType === 'PHONE' ? '(11) 99999-9999'
                        : 'seu@email.com'
                  }
                  className={inputCls}
                />
              )}
            </div>

            <button
              type="submit"
              disabled={createMutation.isPending || !account}
              className="w-full flex items-center justify-center gap-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-xl transition-colors shadow-lg shadow-indigo-500/20"
            >
              {createMutation.isPending ? (
                <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Cadastrando...</>
              ) : (
                <><Plus size={15} /> Cadastrar chave</>
              )}
            </button>
          </form>

          {/* Key list */}
          <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl overflow-hidden">
            <div className="px-5 py-4 border-b border-[#1e1e2e]">
              <h3 className="text-white text-sm font-semibold">Chaves cadastradas</h3>
            </div>

            <div className="divide-y divide-[#1e1e2e]">
              {loadingKeys ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="flex items-center gap-4 px-5 py-4">
                    <Skeleton className="w-10 h-10 rounded-full flex-shrink-0" />
                    <div className="flex-1 space-y-2">
                      <Skeleton className="h-3 w-40" />
                      <Skeleton className="h-2.5 w-24" />
                    </div>
                  </div>
                ))
              ) : !keys?.length ? (
                <div className="py-12 text-center">
                  <KeyRound size={26} className="text-gray-700 mx-auto mb-3" />
                  <p className="text-gray-500 text-sm">Nenhuma chave cadastrada ainda</p>
                  <p className="text-gray-600 text-xs mt-1">Cadastre uma chave acima para receber PIX.</p>
                </div>
              ) : (
                keys.map(key => (
                  <div key={key.id} className="flex items-center gap-4 px-5 py-4 hover:bg-white/[0.02] transition-colors">
                    <div className="w-10 h-10 rounded-full bg-indigo-500/10 flex items-center justify-center flex-shrink-0">
                      <KeyIcon type={key.keyType} />
                    </div>

                    <div className="flex-1 min-w-0">
                      <p className="text-white text-sm font-medium truncate">
                        {formatPixKey(key.keyType, key.keyValue)}
                      </p>
                      <p className="text-gray-600 text-xs">
                        {TYPE_LABEL[key.keyType]} · {formatDate(key.createdAt)}
                      </p>
                    </div>

                    {confirmId === key.id ? (
                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <span className="text-gray-500 text-xs hidden sm:inline">Remover?</span>
                        <button
                          type="button"
                          onClick={() => deleteMutation.mutate(key.id)}
                          disabled={deleteMutation.isPending}
                          title="Confirmar remoção"
                          className="w-8 h-8 rounded-lg bg-red-500/15 text-red-400 hover:bg-red-500/25 flex items-center justify-center transition-colors disabled:opacity-50"
                        >
                          <Check size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmId(null)}
                          title="Cancelar"
                          className="w-8 h-8 rounded-lg bg-white/5 text-gray-400 hover:text-white flex items-center justify-center transition-colors"
                        >
                          <X size={14} />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <button
                          type="button"
                          onClick={() => copyKey(key)}
                          title="Copiar chave"
                          className="w-8 h-8 rounded-lg text-gray-500 hover:text-indigo-400 hover:bg-indigo-500/10 flex items-center justify-center transition-colors"
                        >
                          <Copy size={14} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmId(key.id)}
                          title="Remover chave"
                          className="w-8 h-8 rounded-lg text-gray-500 hover:text-red-400 hover:bg-red-500/10 flex items-center justify-center transition-colors"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
