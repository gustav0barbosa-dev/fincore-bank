import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowDownToLine, CheckCircle, X } from 'lucide-react';
import toast from 'react-hot-toast';
import { getMyAccounts, deposit } from '../api/account';
import { Account } from '../types';

const fmt = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

const inputCls =
  'w-full bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-3.5 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-indigo-500 transition-colors';

export default function Deposit() {
  const [amt, setAmt]         = useState('');
  const [desc, setDesc]       = useState('');
  const [confirm, setConfirm] = useState(false);
  const [done, setDone]       = useState<Account | null>(null);
  const qc = useQueryClient();

  const { data: accounts } = useQuery({ queryKey: ['accounts'], queryFn: getMyAccounts });
  const account = accounts?.[0];

  const mutation = useMutation({
    mutationFn: () => deposit(account!.id, parseFloat(amt), desc || 'Depósito'),
    onSuccess: (updated) => {
      qc.setQueryData<Account[]>(['accounts'], old => old?.map(a => a.id === updated.id ? updated : a));
      qc.invalidateQueries({ queryKey: ['statement'] });
      setConfirm(false);
      setDone(updated);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message ?? 'Erro ao realizar depósito');
      setConfirm(false);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const v = parseFloat(amt);
    if (!v || v <= 0) return toast.error('Informe um valor válido');
    setConfirm(true);
  };

  if (done) return (
    <div className="flex flex-col items-center justify-center py-24 text-center animate-fade-in">
      <div className="w-20 h-20 rounded-full bg-green-400/10 flex items-center justify-center mb-5">
        <CheckCircle size={36} className="text-green-400" />
      </div>
      <h2 className="text-white text-xl font-bold mb-1">Depósito realizado!</h2>
      <p className="text-gray-400 text-sm mb-1">{fmt(parseFloat(amt))} adicionados à sua conta.</p>
      <p className="text-indigo-400 font-semibold text-sm mb-8">
        Novo saldo: {fmt(done.balance)}
      </p>
      <button
        onClick={() => { setAmt(''); setDesc(''); setDone(null); }}
        className="bg-indigo-500 hover:bg-indigo-600 text-white font-semibold px-7 py-2.5 rounded-xl transition-colors"
      >
        Novo depósito
      </button>
    </div>
  );

  return (
    <div className="space-y-6 max-w-md animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Depósito</h1>
        <p className="text-gray-500 text-sm mt-0.5">Adicione saldo à sua conta</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
        {account && (
          <div className="bg-[#0a0a0f] rounded-xl px-4 py-3 space-y-0.5">
            <p className="text-gray-600 text-xs">Saldo atual</p>
            <p className="text-white text-xl font-bold">{fmt(account.balance)}</p>
          </div>
        )}

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
            placeholder="Ex: Salário, transferência recebida..." className={inputCls} />
        </div>

        {/* Quick amounts */}
        <div className="flex gap-2 flex-wrap">
          {[50, 100, 500, 1000].map(v => (
            <button key={v} type="button" onClick={() => setAmt(String(v))}
              className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
                amt === String(v)
                  ? 'border-green-500 bg-green-500/20 text-green-300'
                  : 'border-[#1e1e2e] text-gray-400 hover:border-green-500/50 hover:text-gray-200'
              }`}>
              R$&nbsp;{v}
            </button>
          ))}
        </div>

        <button type="submit" disabled={!account}
          className="w-full flex items-center justify-center gap-2 bg-green-500 hover:bg-green-600 disabled:opacity-50 text-white font-semibold py-3 rounded-xl transition-colors shadow-lg shadow-green-500/20">
          <ArrowDownToLine size={15} />
          Depositar
        </button>
      </form>

      {/* Confirmation modal */}
      {confirm && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-end sm:items-center justify-center p-4 z-50">
          <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 w-full max-w-sm animate-fade-in">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="text-white font-bold text-lg">Confirmar depósito</h3>
                <p className="text-gray-500 text-sm">Verifique os dados antes de continuar</p>
              </div>
              <button onClick={() => setConfirm(false)} className="text-gray-500 hover:text-gray-300 transition-colors mt-0.5">
                <X size={18} />
              </button>
            </div>

            <div className="bg-[#0a0a0f] rounded-xl p-4 space-y-2.5 mb-5 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Valor</span>
                <span className="text-white font-bold text-base">{fmt(parseFloat(amt))}</span>
              </div>
              {desc && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Descrição</span>
                  <span className="text-gray-300">{desc}</span>
                </div>
              )}
              <div className="flex justify-between pt-2 border-t border-[#1e1e2e]">
                <span className="text-gray-500">Novo saldo estimado</span>
                <span className="text-green-400 font-semibold">
                  {fmt((account?.balance ?? 0) + parseFloat(amt))}
                </span>
              </div>
            </div>

            <div className="flex gap-3">
              <button onClick={() => setConfirm(false)}
                className="flex-1 border border-[#1e1e2e] text-gray-300 hover:text-white py-2.5 rounded-xl transition-colors text-sm font-medium">
                Cancelar
              </button>
              <button onClick={() => mutation.mutate()} disabled={mutation.isPending}
                className="flex-1 bg-green-500 hover:bg-green-600 disabled:opacity-50 text-white font-semibold py-2.5 rounded-xl transition-colors text-sm">
                {mutation.isPending ? 'Depositando...' : 'Confirmar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
