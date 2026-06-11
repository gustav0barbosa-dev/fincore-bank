import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, defs, linearGradient, stop,
} from 'recharts';
import { Send, ArrowDownToLine, FileText, Eye, EyeOff, TrendingUp, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import { getMyAccounts, createAccount, getStatement } from '../api/account';
import { useAuth } from '../contexts/AuthContext';
import { CardSkeleton, TransactionSkeleton, ChartSkeleton } from '../components/Skeleton';
import TransactionItem from '../components/TransactionItem';
import { Transaction } from '../types';

const fmt = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

function buildChart(txns: Transaction[]) {
  const days = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - i));
    return {
      label: d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      key: d.toISOString().slice(0, 10),
    };
  });
  const byDay = new Map<string, number>();
  // Sort ascending so the last entry of a day is the closing balance
  [...txns].sort((a, b) => a.createdAt.localeCompare(b.createdAt)).forEach((t) => {
    byDay.set(t.createdAt.slice(0, 10), t.balanceAfter);
  });
  let last = 0;
  return days.map(({ label, key }) => {
    if (byDay.has(key)) last = byDay.get(key)!;
    return { date: label, saldo: last };
  });
}

export default function Dashboard() {
  const [hideBalance, setHideBalance] = useState(false);
  const { user } = useAuth();
  const qc = useQueryClient();

  const { data: accounts, isLoading: loadingAcc } = useQuery({
    queryKey: ['accounts'],
    queryFn: getMyAccounts,
  });

  const account = accounts?.[0];

  const { data: statement, isLoading: loadingStmt } = useQuery({
    queryKey: ['statement', account?.id, 'dash'],
    queryFn: () => getStatement(account!.id, 0, 30),
    enabled: !!account?.id,
  });

  const createMutation = useMutation({
    mutationFn: () => createAccount(user!.fullName, user!.email),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['accounts'] });
      toast.success('Conta criada!');
    },
    onError: () => toast.error('Erro ao criar conta'),
  });

  const chartData = buildChart(statement?.content ?? []);
  const recent   = statement?.content.slice(0, 5) ?? [];
  const firstName = user?.fullName.split(' ')[0];

  if (loadingAcc) return (
    <div className="space-y-4">
      <div className="h-8 w-48 bg-[#1e1e2e] rounded-lg animate-pulse" />
      <CardSkeleton />
      <div className="grid grid-cols-3 gap-3">{[0,1,2].map(i=><CardSkeleton key={i}/>)}</div>
      <ChartSkeleton />
    </div>
  );

  if (!account) return (
    <div className="flex flex-col items-center justify-center py-24 text-center animate-fade-in">
      <div className="w-20 h-20 rounded-2xl bg-indigo-500/10 flex items-center justify-center mb-5">
        <TrendingUp size={32} className="text-indigo-400" />
      </div>
      <h2 className="text-white text-xl font-bold mb-2">Crie sua conta bancária</h2>
      <p className="text-gray-500 text-sm mb-7 max-w-xs">
        Você ainda não tem uma conta. Crie agora gratuitamente e comece a usar todos os recursos.
      </p>
      <button
        onClick={() => createMutation.mutate()}
        disabled={createMutation.isPending}
        className="flex items-center gap-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 text-white font-semibold px-7 py-3 rounded-2xl transition-colors shadow-lg shadow-indigo-500/20"
      >
        <Plus size={17} />
        {createMutation.isPending ? 'Criando...' : 'Criar conta grátis'}
      </button>
    </div>
  );

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Greeting */}
      <div>
        <h1 className="text-2xl font-bold text-white">Olá, {firstName}! 👋</h1>
        <p className="text-gray-500 text-sm mt-0.5">Resumo da sua conta hoje</p>
      </div>

      {/* Balance card */}
      <div className="relative bg-gradient-to-br from-indigo-600 via-indigo-600 to-purple-700 rounded-2xl p-6 overflow-hidden shadow-xl shadow-indigo-900/30">
        <div className="absolute -top-8 -right-8 w-36 h-36 bg-white/5 rounded-full" />
        <div className="absolute -bottom-10 -left-6 w-28 h-28 bg-white/5 rounded-full" />
        <div className="relative">
          <div className="flex items-center justify-between mb-0.5">
            <p className="text-white/60 text-xs font-medium uppercase tracking-wider">Saldo disponível</p>
            <button
              onClick={() => setHideBalance(b => !b)}
              className="text-white/60 hover:text-white transition-colors"
            >
              {hideBalance ? <EyeOff size={14} /> : <Eye size={14} />}
            </button>
          </div>
          <p className="text-3xl font-bold text-white mt-1 mb-5">
            {hideBalance ? '••••••' : fmt(account.balance)}
          </p>
          <div className="flex justify-between text-white/50 text-xs font-medium">
            <span>Ag. {account.agency}</span>
            <span>Conta {account.accountNumber}</span>
          </div>
        </div>
      </div>

      {/* Quick actions */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { to: '/pix',       icon: Send,            label: 'PIX'      },
          { to: '/deposit',   icon: ArrowDownToLine, label: 'Depósito' },
          { to: '/statement', icon: FileText,        label: 'Extrato'  },
        ].map(({ to, icon: Icon, label }) => (
          <Link
            key={to}
            to={to}
            className="group bg-[#12121a] border border-[#1e1e2e] hover:border-indigo-500/40 rounded-2xl p-4 flex flex-col items-center gap-2.5 transition-all hover:shadow-lg hover:shadow-indigo-500/5"
          >
            <div className="w-11 h-11 rounded-xl bg-indigo-500/10 group-hover:bg-indigo-500/20 flex items-center justify-center transition-colors">
              <Icon size={18} className="text-indigo-400" />
            </div>
            <span className="text-gray-300 group-hover:text-white text-xs font-medium transition-colors">{label}</span>
          </Link>
        ))}
      </div>

      {/* Chart */}
      {loadingStmt ? <ChartSkeleton /> : (
        <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-5">
          <h3 className="text-white text-sm font-semibold mb-4 flex items-center gap-2">
            <TrendingUp size={15} className="text-indigo-400" />
            Saldo — últimos 7 dias
          </h3>
          <ResponsiveContainer width="100%" height={150}>
            <LineChart data={chartData} margin={{ top: 4, right: 4, bottom: 0, left: 0 }}>
              <defs>
                <linearGradient id="grad" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stopColor="#6366f1" />
                  <stop offset="100%" stopColor="#a78bfa" />
                </linearGradient>
              </defs>
              <XAxis dataKey="date" tick={{ fill: '#4b5563', fontSize: 10 }} axisLine={false} tickLine={false} />
              <YAxis
                tick={{ fill: '#4b5563', fontSize: 10 }}
                axisLine={false}
                tickLine={false}
                width={50}
                tickFormatter={(v: number) => v >= 1000 ? `R$${(v/1000).toFixed(0)}k` : `R$${v}`}
              />
              <Tooltip
                contentStyle={{ background: '#1a1a2e', border: '1px solid #2a2a3e', borderRadius: '10px', color: '#fff', fontSize: 12 }}
                formatter={(v: number) => [fmt(v), 'Saldo']}
                cursor={{ stroke: '#6366f1', strokeWidth: 1, strokeDasharray: '4 4' }}
              />
              <Line
                type="monotone"
                dataKey="saldo"
                stroke="url(#grad)"
                strokeWidth={2.5}
                dot={false}
                activeDot={{ r: 4, fill: '#6366f1', strokeWidth: 0 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Recent transactions */}
      <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-[#1e1e2e]">
          <h3 className="text-white text-sm font-semibold">Últimas transações</h3>
          <Link to="/statement" className="text-indigo-400 hover:text-indigo-300 text-xs font-medium transition-colors">
            Ver todas →
          </Link>
        </div>
        <div className="divide-y divide-[#1e1e2e] px-3 py-1">
          {loadingStmt
            ? Array.from({length: 5}).map((_,i) => <TransactionSkeleton key={i} />)
            : recent.length === 0
              ? <p className="text-gray-600 text-sm text-center py-8">Nenhuma transação ainda</p>
              : recent.map(t => <TransactionItem key={t.id} transaction={t} />)
          }
        </div>
      </div>
    </div>
  );
}
