import { ArrowDownLeft, ArrowUpRight, Plus, Minus } from 'lucide-react';
import { Transaction } from '../types';

type Cfg = { icon: React.ElementType; label: string; color: string; bg: string; sign: string };

const TYPE_CONFIG: Record<Transaction['type'], Cfg> = {
  DEPOSIT:    { icon: Plus,          label: 'Depósito',      color: 'text-green-400', bg: 'bg-green-400/10',  sign: '+' },
  WITHDRAWAL: { icon: Minus,         label: 'Saque',         color: 'text-red-400',   bg: 'bg-red-400/10',    sign: '-' },
  PIX_IN:     { icon: ArrowDownLeft, label: 'PIX Recebido',  color: 'text-green-400', bg: 'bg-green-400/10',  sign: '+' },
  PIX_OUT:    { icon: ArrowUpRight,  label: 'PIX Enviado',   color: 'text-red-400',   bg: 'bg-red-400/10',    sign: '-' },
  TED_IN:     { icon: ArrowDownLeft, label: 'TED Recebido',  color: 'text-green-400', bg: 'bg-green-400/10',  sign: '+' },
  TED_OUT:    { icon: ArrowUpRight,  label: 'TED Enviado',   color: 'text-red-400',   bg: 'bg-red-400/10',    sign: '-' },
};

const fmt = (v: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);

const fmtDate = (iso: string) =>
  new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit',
  });

export default function TransactionItem({ transaction: t }: { transaction: Transaction }) {
  const cfg = TYPE_CONFIG[t.type] ?? TYPE_CONFIG.DEPOSIT;
  const Icon = cfg.icon;

  return (
    <div className="flex items-center gap-4 py-3 px-2 hover:bg-white/[0.02] rounded-xl transition-colors">
      <div className={`w-10 h-10 rounded-full ${cfg.bg} flex items-center justify-center flex-shrink-0`}>
        <Icon size={17} className={cfg.color} />
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-white text-sm font-medium">{cfg.label}</p>
        <p className="text-gray-500 text-xs truncate">{t.description ?? t.counterpartKey ?? '—'}</p>
      </div>
      <div className="text-right flex-shrink-0">
        <p className={`text-sm font-semibold ${cfg.color}`}>
          {cfg.sign}&nbsp;{fmt(t.amount)}
        </p>
        <p className="text-gray-600 text-xs">{fmtDate(t.createdAt)}</p>
      </div>
    </div>
  );
}
