import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { Home, Send, ArrowDownToLine, FileText, LogOut, Landmark } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

const NAV = [
  { to: '/',           icon: Home,            label: 'Dashboard' },
  { to: '/pix',        icon: Send,            label: 'PIX'       },
  { to: '/deposit',    icon: ArrowDownToLine, label: 'Depósito'  },
  { to: '/statement',  icon: FileText,        label: 'Extrato'   },
];

export default function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { signOut(); navigate('/login', { replace: true }); };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
      isActive
        ? 'bg-indigo-500/15 text-indigo-400 shadow-inner'
        : 'text-gray-400 hover:text-white hover:bg-white/5'
    }`;

  const initial = user?.fullName?.charAt(0).toUpperCase() ?? '?';

  return (
    <div className="flex h-screen bg-[#0a0a0f] overflow-hidden">
      {/* ── Sidebar (desktop) ── */}
      <aside className="hidden md:flex flex-col w-60 bg-[#12121a] border-r border-[#1e1e2e] p-4 flex-shrink-0">
        {/* Brand */}
        <div className="flex items-center gap-2.5 px-2 mb-8 mt-1">
          <div className="w-8 h-8 rounded-xl bg-indigo-500 flex items-center justify-center shadow-lg shadow-indigo-500/30">
            <Landmark size={15} className="text-white" />
          </div>
          <span className="text-white font-bold text-lg tracking-tight">FinCore</span>
        </div>

        {/* Nav */}
        <nav className="flex-1 space-y-0.5">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink key={to} to={to} end={to === '/'} className={linkClass}>
              <Icon size={17} />
              {label}
            </NavLink>
          ))}
        </nav>

        {/* User + logout */}
        <div className="border-t border-[#1e1e2e] pt-4 space-y-1">
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
              {initial}
            </div>
            <div className="min-w-0">
              <p className="text-white text-sm font-medium truncate">{user?.fullName}</p>
              <p className="text-gray-500 text-xs truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-gray-400 hover:text-red-400 hover:bg-red-400/10 transition-colors"
          >
            <LogOut size={17} />
            Sair
          </button>
        </div>
      </aside>

      {/* ── Main area ── */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Mobile top bar */}
        <header className="md:hidden flex items-center justify-between px-4 py-3 bg-[#12121a] border-b border-[#1e1e2e] flex-shrink-0">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 rounded-lg bg-indigo-500 flex items-center justify-center">
              <Landmark size={13} className="text-white" />
            </div>
            <span className="text-white font-bold">FinCore</span>
          </div>
          <button onClick={handleLogout} className="text-gray-400 hover:text-red-400 transition-colors p-1">
            <LogOut size={18} />
          </button>
        </header>

        {/* Scrollable content */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 animate-fade-in">
          <Outlet />
        </main>

        {/* Mobile bottom nav */}
        <nav className="md:hidden flex bg-[#12121a] border-t border-[#1e1e2e] flex-shrink-0">
          {NAV.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) =>
                `flex-1 flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium transition-colors ${
                  isActive ? 'text-indigo-400' : 'text-gray-500 hover:text-gray-300'
                }`
              }
            >
              <Icon size={19} />
              {label}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  );
}
