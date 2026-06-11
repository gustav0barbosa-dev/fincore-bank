import { Link } from 'react-router-dom';
import { Home, MoveLeft } from 'lucide-react';

export default function NotFound() {
  return (
    <div className="min-h-screen bg-[#0a0a0f] flex items-center justify-center p-4">
      <div className="text-center animate-fade-in">
        <p className="text-8xl font-black text-[#1e1e2e] mb-2 select-none">404</p>
        <h1 className="text-2xl font-bold text-white mb-2">Página não encontrada</h1>
        <p className="text-gray-500 text-sm mb-8 max-w-xs mx-auto">
          A página que você está procurando não existe ou foi movida para outro endereço.
        </p>
        <div className="flex items-center justify-center gap-3">
          <button
            onClick={() => window.history.back()}
            className="flex items-center gap-2 border border-[#1e1e2e] text-gray-300 hover:text-white px-5 py-2.5 rounded-xl transition-colors text-sm font-medium"
          >
            <MoveLeft size={15} />
            Voltar
          </button>
          <Link
            to="/"
            className="flex items-center gap-2 bg-indigo-500 hover:bg-indigo-600 text-white font-semibold px-5 py-2.5 rounded-xl transition-colors text-sm shadow-lg shadow-indigo-500/20"
          >
            <Home size={15} />
            Ir para o início
          </Link>
        </div>
      </div>
    </div>
  );
}
