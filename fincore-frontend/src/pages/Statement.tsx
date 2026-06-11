import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, SlidersHorizontal } from 'lucide-react';
import { getMyAccounts, getStatement } from '../api/account';
import { TransactionSkeleton } from '../components/Skeleton';
import TransactionItem from '../components/TransactionItem';
import { Transaction } from '../types';

type Filter = 'ALL' | Transaction['type'];

const FILTERS: { value: Filter; label: string }[] = [
  { value: 'ALL',      label: 'Todas'         },
  { value: 'DEPOSIT',  label: 'Depósitos'      },
  { value: 'PIX_OUT',  label: 'PIX Enviado'    },
  { value: 'PIX_IN',   label: 'PIX Recebido'   },
  { value: 'TED_OUT',  label: 'TED Enviado'    },
  { value: 'TED_IN',   label: 'TED Recebido'   },
];

export default function Statement() {
  const [page, setPage]     = useState(0);
  const [filter, setFilter] = useState<Filter>('ALL');
  const PAGE_SIZE = 15;

  const { data: accounts } = useQuery({ queryKey: ['accounts'], queryFn: getMyAccounts });
  const account = accounts?.[0];

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['statement', account?.id, page],
    queryFn: () => getStatement(account!.id, page, PAGE_SIZE),
    enabled: !!account?.id,
    placeholderData: prev => prev,
  });

  const filtered = filter === 'ALL'
    ? (data?.content ?? [])
    : (data?.content ?? []).filter(t => t.type === filter);

  const changeFilter = (f: Filter) => { setFilter(f); setPage(0); };

  return (
    <div className="space-y-5 max-w-2xl animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Extrato</h1>
        <p className="text-gray-500 text-sm mt-0.5">Histórico completo de movimentações</p>
      </div>

      {/* Filters */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
        <SlidersHorizontal size={13} className="text-gray-500 flex-shrink-0" />
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            onClick={() => changeFilter(value)}
            className={`text-xs font-medium px-3.5 py-1.5 rounded-full whitespace-nowrap flex-shrink-0 transition-colors border ${
              filter === value
                ? 'bg-indigo-500 border-indigo-500 text-white'
                : 'bg-transparent border-[#1e1e2e] text-gray-400 hover:text-white hover:border-indigo-500/40'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className={`bg-[#12121a] border border-[#1e1e2e] rounded-2xl overflow-hidden transition-opacity ${isFetching ? 'opacity-70' : ''}`}>
        <div className="divide-y divide-[#1e1e2e] px-3 py-1">
          {isLoading
            ? Array.from({length: 8}).map((_,i) => <TransactionSkeleton key={i} />)
            : filtered.length === 0
              ? (
                <div className="py-14 text-center">
                  <p className="text-gray-500 text-sm">
                    {filter === 'ALL' ? 'Nenhuma transação encontrada' : `Sem transações do tipo "${FILTERS.find(f=>f.value===filter)?.label}"`}
                  </p>
                </div>
              )
              : filtered.map(t => <TransactionItem key={t.id} transaction={t} />)
          }
        </div>

        {/* Pagination */}
        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between px-5 py-3.5 border-t border-[#1e1e2e]">
            <p className="text-gray-600 text-xs">
              Pág. {page + 1} / {data.totalPages} &middot; {data.totalElements} registros
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={data.first}
                className="w-8 h-8 rounded-lg bg-[#0a0a0f] border border-[#1e1e2e] flex items-center justify-center text-gray-400 hover:text-white hover:border-indigo-500/40 disabled:opacity-30 transition-colors"
              >
                <ChevronLeft size={15} />
              </button>
              <button
                onClick={() => setPage(p => p + 1)}
                disabled={data.last}
                className="w-8 h-8 rounded-lg bg-[#0a0a0f] border border-[#1e1e2e] flex items-center justify-center text-gray-400 hover:text-white hover:border-indigo-500/40 disabled:opacity-30 transition-colors"
              >
                <ChevronRight size={15} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
