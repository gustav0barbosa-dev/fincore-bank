import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Landmark } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { register } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';

const fmtCpf = (v: string) =>
  v.replace(/\D/g, '').slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');

export default function Register() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail]       = useState('');
  const [cpf, setCpf]           = useState('');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd]   = useState(false);
  const { signIn }              = useAuth();
  const navigate                = useNavigate();

  const mutation = useMutation({
    mutationFn: () => register(fullName, email, password, cpf.replace(/\D/g, '')),
    onSuccess: (data) => {
      signIn(data.accessToken, { email: data.email, fullName: data.fullName, role: data.role });
      toast.success('Conta criada com sucesso!');
      navigate('/', { replace: true });
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message ?? 'Erro ao criar conta');
    },
  });

  const inputCls =
    'w-full bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-3.5 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-indigo-500 transition-colors';

  return (
    <div className="min-h-screen bg-[#0a0a0f] flex items-center justify-center p-4">
      <div className="w-full max-w-sm animate-fade-in">
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 rounded-2xl bg-indigo-500 flex items-center justify-center mb-4 shadow-xl shadow-indigo-500/30">
            <Landmark size={26} className="text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">FinCore</h1>
          <p className="text-gray-500 text-sm mt-1">Crie sua conta em segundos</p>
        </div>

        <form
          onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}
          className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4"
        >
          {[
            { label: 'Nome completo', value: fullName, set: setFullName, type: 'text', ph: 'João da Silva', minLength: 3 },
            { label: 'Email',         value: email,    set: setEmail,    type: 'email', ph: 'seu@email.com' },
          ].map(({ label, value, set, type, ph, minLength }) => (
            <div key={label}>
              <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">{label}</label>
              <input
                type={type}
                value={value}
                onChange={(e) => set(e.target.value)}
                required
                minLength={minLength}
                placeholder={ph}
                className={inputCls}
              />
            </div>
          ))}

          <div>
            <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">CPF</label>
            <input
              value={cpf}
              onChange={(e) => setCpf(fmtCpf(e.target.value))}
              required
              placeholder="000.000.000-00"
              className={inputCls}
            />
          </div>

          <div>
            <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">Senha</label>
            <div className="relative">
              <input
                type={showPwd ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
                placeholder="Mínimo 8 caracteres"
                className={`${inputCls} pr-10`}
              />
              <button
                type="button"
                onClick={() => setShowPwd(!showPwd)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
              >
                {showPwd ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-xl transition-colors shadow-lg shadow-indigo-500/20 mt-2"
          >
            {mutation.isPending ? 'Criando conta...' : 'Criar conta'}
          </button>
        </form>

        <p className="text-center text-gray-600 text-sm mt-5">
          Já tem conta?{' '}
          <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
            Entrar
          </Link>
        </p>
      </div>
    </div>
  );
}
