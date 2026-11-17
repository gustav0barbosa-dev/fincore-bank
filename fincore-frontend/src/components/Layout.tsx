import { NavLink, Outlet, useLocation, useNavigate, Link } from 'react-router-dom';
import {
  Home, Send, ArrowDownToLine, FileText, LogOut, Landmark, KeyRound,
  ChevronRight, ShieldCheck, Sparkles,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const NAV = [
  { to: '/',           icon: Home,            label: 'Visão geral', mobileLabel: 'Início' },
  { to: '/pix',        icon: Send,            label: 'Transferir via PIX', mobileLabel: 'PIX' },
  { to: '/pix-keys',   icon: KeyRound,        label: 'Minhas chaves', mobileLabel: 'Chaves' },
  { to: '/deposit',    icon: ArrowDownToLine, label: 'Adicionar dinheiro', mobileLabel: 'Depositar' },
  { to: '/statement',  icon: FileText,        label: 'Extrato', mobileLabel: 'Extrato' },
];

const PAGE_TITLES: Record<string, string> = {
  '/': 'Visão geral',
  '/pix': 'Transferências PIX',
  '/pix-keys': 'Minhas chaves PIX',
  '/deposit': 'Adicionar dinheiro',
  '/statement': 'Extrato',
  '/profile': 'Meu perfil',
};

export default function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    signOut();
    navigate('/login', { replace: true });
  };

  const initial = user?.fullName?.charAt(0).toUpperCase() ?? '?';
  const pageTitle = PAGE_TITLES[location.pathname] ?? 'FinCore';

  const navClass = ({ isActive }: { isActive: boolean }) =>
    [
      'group relative flex items-center gap-3 rounded-xl px-3.5 py-3 text-[13px] font-medium transition-all duration-200',
      isActive
        ? 'bg-violet-400/[0.12] text-violet-200 shadow-[inset_0_0_0_1px_rgba(130,117,255,0.13)]'
        : 'text-slate-400 hover:bg-white/[0.045] hover:text-slate-100',
    ].join(' ');

  return (
    <div className="min-h-screen bg-[#090b13] text-slate-100 md:flex">
      {/* Desktop navigation */}
      <aside className="hidden w-[264px] shrink-0 flex-col border-r border-white/[0.07] bg-[#0d101b] px-4 py-5 md:flex">
        <Link to="/" className="mb-10 flex items-center gap-3 px-2" aria-label="FinCore — início">
          <span className="flex h-10 w-10 items-center justify-center rounded-[14px] bg-gradient-to-br from-violet-500 to-indigo-600 shadow-lg shadow-violet-950/50">
            <Landmark size={19} strokeWidth={2.2} className="text-white" />
          </span>
          <span className="min-w-0">
            <span className="block text-[17px] font-bold tracking-[-0.04em] text-white">FinCore</span>
            <span className="mt-0.5 block text-[9px] font-semibold uppercase tracking-[0.22em] text-slate-500">Digital banking</span>
          </span>
        </Link>

        <div className="mb-3 px-3 text-[10px] font-bold uppercase tracking-[0.18em] text-slate-600">
          Menu principal
        </div>
        <nav aria-label="Navegação principal" className="flex-1 space-y-1">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to} end={to === '/'} className={navClass}>
              {({ isActive }) => (
                <>
                  {isActive && <span className="absolute inset-y-2 left-0 w-[3px] rounded-r-full bg-violet-400" />}
                  <Icon size={17} strokeWidth={1.8} className={isActive ? 'text-violet-300' : 'text-slate-500 group-hover:text-slate-300'} />
                  <span>{label}</span>
                  {isActive && <ChevronRight size={14} className="ml-auto text-violet-300/70" />}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        <div className="mb-4 rounded-2xl border border-violet-400/10 bg-gradient-to-br from-violet-500/[0.10] to-cyan-400/[0.04] p-4">
          <div className="mb-2 flex items-center gap-2 text-violet-200">
            <Sparkles size={14} />
            <span className="text-[11px] font-semibold">FinCore Experience</span>
          </div>
          <p className="text-[11px] leading-relaxed text-slate-500">
            Sua experiência financeira, simples e conectada.
          </p>
        </div>

        <div className="border-t border-white/[0.07] pt-4">
          <Link
            to="/profile"
            title="Meu perfil"
            className="group flex items-center gap-3 rounded-xl p-2.5 transition-colors hover:bg-white/[0.045]"
          >
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-violet-300/20 bg-violet-400/10 text-sm font-bold text-violet-200">
              {initial}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-xs font-semibold text-slate-200">{user?.fullName}</span>
              <span className="mt-0.5 block truncate text-[10px] text-slate-500">{user?.email}</span>
            </span>
            <ChevronRight size={14} className="shrink-0 text-slate-600 transition-transform group-hover:translate-x-0.5" />
          </Link>
          <button
            onClick={handleLogout}
            className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-xs font-medium text-slate-500 transition-colors hover:bg-rose-400/[0.08] hover:text-rose-300"
          >
            <LogOut size={16} />
            Encerrar sessão
          </button>
        </div>
      </aside>

      {/* Main application area */}
      <div className="flex min-h-screen min-w-0 flex-1 flex-col">
        {/* Desktop top bar */}
        <header className="sticky top-0 z-20 hidden h-[76px] items-center justify-between border-b border-white/[0.06] bg-[#090b13]/85 px-8 backdrop-blur-xl md:flex">
          <div>
            <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-slate-600">FinCore / Conta digital</p>
            <h1 className="mt-1 text-sm font-semibold tracking-tight text-slate-100">{pageTitle}</h1>
          </div>
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-2 rounded-full border border-emerald-400/10 bg-emerald-400/[0.06] px-3 py-1.5 text-[10px] font-medium text-emerald-300/90">
              <ShieldCheck size={13} />
              Ambiente de demonstração
            </span>
            <Link to="/profile" className="flex h-9 w-9 items-center justify-center rounded-full border border-white/10 bg-white/[0.05] text-xs font-semibold text-slate-200 transition-colors hover:border-violet-300/30 hover:bg-violet-400/10" aria-label="Abrir meu perfil">
              {initial}
            </Link>
          </div>
        </header>

        {/* Mobile top bar */}
        <header className="sticky top-0 z-20 flex h-[62px] shrink-0 items-center justify-between border-b border-white/[0.07] bg-[#0d101b]/95 px-4 backdrop-blur-xl md:hidden">
          <Link to="/" className="flex items-center gap-2.5" aria-label="FinCore — início">
            <span className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600">
              <Landmark size={15} className="text-white" />
            </span>
            <span className="text-[15px] font-bold tracking-tight text-white">FinCore</span>
          </Link>
          <div className="flex items-center gap-3">
            <span className="text-[10px] font-medium text-slate-500">{pageTitle}</span>
            <Link
              to="/profile"
              title="Meu perfil"
              className="flex h-8 w-8 items-center justify-center rounded-full border border-violet-300/20 bg-violet-400/10 text-xs font-bold text-violet-200"
            >
              {initial}
            </Link>
            <button onClick={handleLogout} aria-label="Encerrar sessão" className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-rose-400/10 hover:text-rose-300">
              <LogOut size={17} />
            </button>
          </div>
        </header>

        <main className="w-full flex-1 px-4 py-6 pb-24 sm:px-6 md:px-8 md:py-8 md:pb-8">
          <div className="mx-auto w-full max-w-[1440px] animate-fade-in">
            <Outlet />
          </div>
        </main>

        {/* Mobile bottom navigation */}
        <nav aria-label="Navegação mobile" className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t border-white/[0.08] bg-[#0d101b]/95 px-1 pb-[max(env(safe-area-inset-bottom),0.35rem)] pt-2 backdrop-blur-xl md:hidden">
          {NAV.map(({ to, icon: Icon, mobileLabel }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                [
                  'flex min-w-0 flex-col items-center gap-1 rounded-xl py-1.5 text-[9px] font-medium transition-colors',
                  isActive ? 'text-violet-300' : 'text-slate-600 hover:text-slate-300',
                ].join(' ')
              }
            >
              {({ isActive }) => (
                <>
                  <Icon size={18} strokeWidth={isActive ? 2.2 : 1.7} />
                  <span className="truncate">{mobileLabel}</span>
                </>
              )}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  );
}
