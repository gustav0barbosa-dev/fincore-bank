import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, Eye, EyeOff, Landmark } from 'lucide-react';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { register } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';
import { formatCpf, formatPhone, onlyDigits } from '../utils/format';
import { isStrongPassword, isValidCpf, isValidPhone, passwordRules } from '../utils/validation';
import { apiErrorMessage } from '../utils/errors';

export default function Register() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail]       = useState('');
  const [cpf, setCpf]           = useState('');
  const [phone, setPhone]       = useState('');
  const [password, setPassword] = useState('');
  const [transactionPin, setTransactionPin] = useState('');
  const [showPwd, setShowPwd]   = useState(false);
  const { signIn }              = useAuth();
  const navigate                = useNavigate();

  const mutation = useMutation({
    mutationFn: () => register(
      fullName.trim(),
      email.trim(),
      password,
      onlyDigits(cpf),
      transactionPin,
      onlyDigits(phone) || undefined,
    ),
    onSuccess: (data) => {
      signIn(data.accessToken, { email: data.email, fullName: data.fullName, role: data.role ?? 'USER' });
      toast.success('Conta criada com sucesso!');
      navigate('/', { replace: true });
    },
    onError: (err) => {
      toast.error(apiErrorMessage(err, 'Erro ao criar conta'));
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (fullName.trim().length < 3) return toast.error('Informe seu nome completo');
    if (!isValidCpf(cpf)) return toast.error('CPF inválido');
    if (phone && !isValidPhone(phone)) return toast.error('Telefone deve ter 10 ou 11 dígitos');
    if (!isStrongPassword(password)) return toast.error('A senha não atende aos requisitos');
    if (!/^\d{4}$/.test(transactionPin)) return toast.error('A senha de transação deve ter 4 dígitos');
    mutation.mutate();
  };

  const cpfComplete = onlyDigits(cpf).length === 11;
  const cpfInvalid = cpfComplete && !isValidCpf(cpf);
  const rules = passwordRules(password);

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
          onSubmit={handleSubmit}
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
              onChange={(e) => setCpf(formatCpf(e.target.value))}
              required
              inputMode="numeric"
              placeholder="000.000.000-00"
              className={`${inputCls} ${cpfInvalid ? 'border-red-500/60' : ''}`}
            />
            {cpfInvalid && (
              <p className="text-red-400 text-xs mt-1.5">CPF inválido. Confira os números digitados.</p>
            )}
          </div>

          <div>
            <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
              Telefone <span className="text-gray-600 normal-case">(opcional)</span>
            </label>
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(formatPhone(e.target.value))}
              placeholder="(11) 99999-9999"
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
                maxLength={100}
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
            {password.length > 0 && (
              <ul className="mt-2 space-y-1">
                {rules.map(rule => (
                  <li
                    key={rule.label}
                    className={`flex items-center gap-1.5 text-xs ${rule.ok ? 'text-green-400' : 'text-gray-600'}`}
                  >
                    <CheckCircle2 size={12} className={rule.ok ? 'opacity-100' : 'opacity-30'} />
                    {rule.label}
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div>
            <label className="text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide">
              Senha de transação
            </label>
            <input
              type="password"
              inputMode="numeric"
              value={transactionPin}
              onChange={(e) => setTransactionPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
              required
              pattern="[0-9]{4}"
              maxLength={4}
              placeholder="4 dígitos"
              className={inputCls}
            />
            <p className="text-gray-600 text-xs mt-1.5">
              Você usará este PIN para autorizar transferências PIX.
            </p>
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
