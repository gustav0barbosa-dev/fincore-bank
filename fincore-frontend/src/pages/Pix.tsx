import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, Clock, CheckCircle, XCircle, AlertCircle, KeyRound } from 'lucide-react';
import toast from 'react-hot-toast';
import { getMyAccounts } from '../api/account';
import { sendPix, getPixHistory } from '../api/transaction';
import { TransactionSkeleton } from '../components/Skeleton';
import { PixTransaction } from '../types';
import { apiErrorMessage } from '../utils/errors';

const fmt = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

const fmtDate = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });

const STATUS: Record<PixTransaction['status'], { icon: React.ElementType; color: string; label: string }> = {
  PENDING:    { icon: Clock,         color: 'text-yellow-400', label: 'Pendente'     },
  PROCESSING: { icon: AlertCircle,   color: 'text-blue-400',   label: 'Processando'  },
  COMPLETED:  { icon: CheckCircle,   color: 'text-green-400',  label: 'Concluído'    },
  FAILED:     { icon: XCircle,       color: 'text-red-400',    label: 'Falhou'       },
};

const inputCls =
  'w-full bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-3.5 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-indigo-500 transition-colors';

export default function Pix() {
  const [key, setKey]   = useState('');
  const [amt, setAmt]   = useState('');
  const [desc, setDesc] = useState('');
  const [pin, setPin]   = useState('');
  const qc = useQueryClient();

  const { data: accounts } = useQuery({ queryKey: ['accounts'], queryFn: getMyAccounts });
  const account = accounts?.[0];

  const { data: history, isLoading } = useQuery({
    queryKey: ['pix-history', account?.id],
    queryFn: () => getPixHistory(account!.id, 0, 20),
    enabled: !!account?.id,
  });

  const mutation = useMutation({
    mutationFn: () => sendPix(account!.id, key.trim(), parseFloat(amt), pin, desc || undefined),
    onSuccess: () => {
      toast.success('PIX enviado! Aguarde a confirmação.');
      setKey(''); setAmt(''); setDesc(''); setPin('');
      qc.invalidateQueries({ queryKey: ['pix-history'] });
      qc.invalidateQueries({ queryKey: ['statement'] });
      qc.invalidateQueries({ queryKey: ['accounts'] });
    },
    onError: (err) =>
      toast.error(apiErrorMessage(err, 'Erro ao enviar PIX')),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!key.trim()) return toast.error('Informe a chave PIX');
    const v = parseFloat(amt);
    if (!v || v <= 0) return toast.error('Valor inválido');
    if (!/^\d{4}$/.test(pin)) return toast.error('Informe a senha de transação de 4 dígitos');
    mutation.mutate();
  };

  return (
    <div className="space-y-6 max-w-xl animate-fade-in">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-white">Transferir via PIX</h1>
          <p className="text-gray-500 text-sm mt-0.5">Envio instantâneo, 24h por dia</p>
        </div>
        <Link
          to="/pix-keys"
          className="flex items-center gap-1.5 text-indigo-400 hover:text-indigo-300 text-xs font-medium bg-indigo-500/10 hover:bg-indigo-500/15 px-3 py-2 rounded-xl transition-colors flex-shrink-0"
        >
          <KeyRound size={13} />
          Minhas chaves
        </Link>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
        {account && (
          <div className="flex justify-between items-center bg-[#0a0a0f] rounded-xl px-4 py-2.5 text-sm">
            <span className="text-gray-500">Saldo disponível</span>
            <span className="text-indigo-400 font-semibold">{fmt(account.balance)}</span>
          </div>
        )}

        <div>
          <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
            Chave PIX do destinatário
          </label>
          <input value={key} onChange={e => setKey(e.target.value)} required
            placeholder="Email, CPF, telefone ou chave aleatória" className={inputCls} />
          <p className="text-gray-600 text-xs mt-1.5">
            A chave precisa estar cadastrada no FinCore pelo destinatário.
          </p>
        </div>

        <div>
          <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
            Valor (R$)
          </label>
          <input type="number" min="0.01" step="0.01" value={amt}
            onChange={e => setAmt(e.target.value)} required placeholder="0,00" className={inputCls} />
        </div>

        <div>
          <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
            Descrição <span className="text-gray-600 normal-case">(opcional)</span>
          </label>
          <input value={desc} onChange={e => setDesc(e.target.value)}
            placeholder="Para que é esse PIX?" className={inputCls} />
        </div>

        <div>
          <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
            Senha de transação
          </label>
          <input
            type="password"
            inputMode="numeric"
            value={pin}
            onChange={e => setPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
            required
            pattern="[0-9]{4}"
            maxLength={4}
            placeholder="4 dígitos"
            className={inputCls}
          />
          <p className="text-gray-600 text-xs mt-1.5">
            PIN cadastrado no seu{' '}
            <Link to="/profile" className="text-indigo-400 hover:text-indigo-300 transition-colors">
              perfil
            </Link>
            , usado para autorizar esta transferência.
          </p>
        </div>

        {/* Quick amounts */}
        <div className="flex gap-2 flex-wrap">
          {[10, 50, 100, 500].map(v => (
            <button key={v} type="button" onClick={() => setAmt(String(v))}
              className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
                amt === String(v)
                  ? 'border-indigo-500 bg-indigo-500/20 text-indigo-300'
                  : 'border-[#1e1e2e] text-gray-400 hover:border-indigo-500/50 hover:text-gray-200'
              }`}>
              R$&nbsp;{v}
            </button>
          ))}
        </div>

        <button type="submit" disabled={mutation.isPending || !account}
          className="w-full flex items-center justify-center gap-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-3 rounded-xl transition-colors shadow-lg shadow-indigo-500/20">
          {mutation.isPending ? (
            <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Enviando...</>
          ) : (
            <><Send size={15} /> Enviar PIX</>
          )}
        </button>
      </form>

      {/* History */}
      <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl overflow-hidden">
        <div className="px-5 py-4 border-b border-[#1e1e2e]">
          <h3 className="text-white text-sm font-semibold">Histórico de PIX enviados</h3>
        </div>
        <div className="divide-y divide-[#1e1e2e]">
          {isLoading
            ? Array.from({length: 3}).map((_,i) => <TransactionSkeleton key={i} />)
            : !history?.content.length
              ? <p className="text-gray-600 text-sm text-center py-8">Nenhum PIX enviado ainda</p>
              : history.content.map(pix => {
                  const s = STATUS[pix.status] ?? STATUS.PENDING;
                  const Icon = s.icon;
                  return (
                    <div key={pix.transactionId} className="flex items-center gap-4 px-5 py-4 hover:bg-white/[0.02] transition-colors">
                      <div className="w-10 h-10 rounded-full bg-red-400/10 flex items-center justify-center flex-shrink-0">
                        <Send size={15} className="text-red-400" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-white text-sm font-medium truncate">{pix.receiverKey}</p>
                        <p className="text-gray-600 text-xs">{pix.description ?? fmtDate(pix.createdAt)}</p>
                      </div>
                      <div className="text-right flex-shrink-0">
                        <p className="text-red-400 text-sm font-semibold">− {fmt(pix.amount)}</p>
                        <span className={`flex items-center justify-end gap-0.5 text-[11px] ${s.color}`}>
                          <Icon size={10} /> {s.label}
                        </span>
                      </div>
                    </div>
                  );
                })
          }
        </div>
      </div>
    </div>
  );
}
