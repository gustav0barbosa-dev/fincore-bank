export function Skeleton({ className = '' }: { className?: string }) {
  return <div className={`animate-pulse bg-[#1e1e2e] rounded-lg ${className}`} />;
}

export function CardSkeleton() {
  return (
    <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-3">
      <Skeleton className="h-3 w-20" />
      <Skeleton className="h-7 w-36" />
      <Skeleton className="h-3 w-28" />
    </div>
  );
}

export function TransactionSkeleton() {
  return (
    <div className="flex items-center gap-4 py-3 px-2">
      <Skeleton className="w-10 h-10 rounded-full flex-shrink-0" />
      <div className="flex-1 space-y-2">
        <Skeleton className="h-3 w-28" />
        <Skeleton className="h-2.5 w-20" />
      </div>
      <div className="space-y-2 text-right">
        <Skeleton className="h-3 w-20" />
        <Skeleton className="h-2.5 w-16" />
      </div>
    </div>
  );
}

export function ChartSkeleton() {
  return (
    <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-5">
      <Skeleton className="h-4 w-40 mb-4" />
      <Skeleton className="h-40 w-full rounded-xl" />
    </div>
  );
}
