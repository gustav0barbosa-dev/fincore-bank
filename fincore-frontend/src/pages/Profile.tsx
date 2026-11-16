import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Calendar, CheckCircle2, CreditCard, Eye, EyeOff, Fingerprint, KeyRound, Lock,
  Mail, Phone, Save, ShieldCheck, UserRound, XCircle,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { changePassword, changeTransactionPin, getProfile, updateProfile } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';
import { CardSkeleton } from '../components/Skeleton';
import { formatCpf, formatDate, formatPhone, onlyDigits } from '../utils/format';
import { isStrongPassword, isValidPhone, passwordRules } from '../utils/validation';
import { apiErrorMessage } from '../utils/errors';

const inputCls =
  'w-full bg-[#0a0a0f] border border-[#1e1e2e] rounded-xl px-3.5 py-2.5 text-white text-sm placeholder-gray-600 focus:outline-none focus:border-indigo-500 transition-colors';

const labelCls = 'text-gray-400 text-xs font-medium block mb-1.5 uppercase tracking-wide';

function PasswordField({
  label, value, onChange, placeholder, autoComplete,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  autoComplete: string;
}) {
  const [visible, setVisible] = useState(false);
  return (
    <div>
      <label className={labelCls}>{label}</label>
      <div className="relative">
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={e => onChange(e.target.value)}
          required
          autoComplete={autoComplete}
          placeholder={placeholder}
          className={`${inputCls} pr-10`}
        />
        <button
          type="button"
          onClick={() => setVisible(v => !v)}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300 transition-colors"
          aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
        >
          {visible ? <EyeOff size={15} /> : <Eye size={15} />}
        </button>
      </div>
    </div>
  );
}

function InfoRow({ icon: Icon, label, value }: { icon: React.ElementType; label: string; value: string }) {
  return (
    <div className="flex items-start gap-3">
      <Icon size={15} className="text-gray-600 mt-0.5 flex-shrink-0" />
      <div className="min-w-0">
        <p className="text-gray-600 text-[11px] uppercase tracking-wide">{label}</p>
        <p className="text-gray-200 text-sm truncate">{value}</p>
      </div>
    </div>
  );
}

export default function Profile() {
  const { user, updateUser } = useAuth();
  const qc = useQueryClient();

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [pinPassword, setPinPassword] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    staleTime: 60_000,
  });

  useEffect(() => {
    if (!profile) return;
    setFullName(profile.fullName);
    setPhone(profile.phone ? formatPhone(profile.phone) : '');
  }, [profile?.id]);

  const profileMutation = useMutation({
    mutationFn: () => updateProfile(fullName.trim(), onlyDigits(phone) || undefined),
    onSuccess: (updated) => {
      toast.success('Perfil atualizado!');
      qc.setQueryData(['profile'], updated);
      updateUser({ fullName: updated.fullName });
      setPhone(updated.phone ? formatPhone(updated.phone) : '');
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Erro ao atualizar perfil')),
  });

  const passwordMutation = useMutation({
    mutationFn: () => changePassword(currentPassword, newPassword),
    onSuccess: () => {
      toast.success('Senha alterada com sucesso!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Erro ao alterar senha')),
  });

  const pinMutation = useMutation({
    mutationFn: () => changeTransactionPin(pinPassword, newPin),
    onSuccess: () => {
      toast.success('Senha de transação alterada!');
      setPinPassword('');
      setNewPin('');
      setConfirmPin('');
      qc.invalidateQueries({ queryKey: ['profile'] });
    },
    onError: (err) => toast.error(apiErrorMessage(err, 'Erro ao alterar senha de transação')),
  });

  const handleProfileSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (fullName.trim().length < 3) return toast.error('Informe seu nome completo');
    if (phone && !isValidPhone(phone)) return toast.error('Telefone deve ter 10 ou 11 dígitos');
    profileMutation.mutate();
  };

  const handlePasswordSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword) return toast.error('Informe sua senha atual');
    if (!isStrongPassword(newPassword)) return toast.error('A nova senha não atende aos requisitos');
    if (newPassword !== confirmPassword) return toast.error('As senhas não coincidem');
    passwordMutation.mutate();
  };

  const handlePinSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!pinPassword) return toast.error('Informe sua senha de acesso');
    if (!/^\d{4}$/.test(newPin)) return toast.error('O PIN deve ter exatamente 4 dígitos');
    if (newPin !== confirmPin) return toast.error('Os PINs não coincidem');
    pinMutation.mutate();
  };

  if (isLoading) {
    return (
      <div className="space-y-4 max-w-3xl">
        <div className="h-8 w-40 bg-[#1e1e2e] rounded-lg animate-pulse" />
        <CardSkeleton />
        <CardSkeleton />
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="py-20 text-center">
        <p className="text-gray-500 text-sm">Não foi possível carregar seu perfil.</p>
      </div>
    );
  }

  const initial = profile.fullName.charAt(0).toUpperCase();
  const rules = passwordRules(newPassword);

  return (
    <div className="space-y-6 max-w-3xl animate-fade-in">
      <div>
        <h1 className="text-2xl font-bold text-white">Meu perfil</h1>
        <p className="text-gray-500 text-sm mt-0.5">Gerencie seus dados e credenciais de segurança</p>
      </div>

      {/* Summary */}
      <div className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6">
        <div className="flex items-center gap-4 mb-6">
          <div className="w-14 h-14 rounded-2xl bg-indigo-500 flex items-center justify-center text-white text-xl font-bold shadow-lg shadow-indigo-500/25 flex-shrink-0">
            {initial}
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-white font-semibold truncate">{profile.fullName}</h2>
              <span className="text-[10px] font-semibold uppercase tracking-wide text-indigo-300 bg-indigo-500/15 px-2 py-0.5 rounded-full">
                {profile.role}
              </span>
            </div>
            <p className="text-gray-500 text-sm truncate">{profile.email}</p>
          </div>
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          <InfoRow icon={Mail} label="E-mail" value={profile.email} />
          <InfoRow icon={CreditCard} label="CPF" value={profile.cpf ? formatCpf(profile.cpf) : 'Não informado'} />
          <InfoRow icon={Phone} label="Telefone" value={profile.phone ? formatPhone(profile.phone) : 'Não informado'} />
          <InfoRow icon={Calendar} label="Cliente desde" value={formatDate(profile.createdAt)} />
          <InfoRow icon={Fingerprint} label="ID do usuário" value={`#${profile.id}`} />
          <div className="flex items-start gap-3">
            <ShieldCheck size={15} className="text-gray-600 mt-0.5 flex-shrink-0" />
            <div>
              <p className="text-gray-600 text-[11px] uppercase tracking-wide">Senha de transação</p>
              <span className={`inline-flex items-center gap-1 text-sm ${
                profile.transactionPinConfigured ? 'text-green-400' : 'text-yellow-400'
              }`}>
                {profile.transactionPinConfigured
                  ? <><CheckCircle2 size={13} /> Configurada</>
                  : <><XCircle size={13} /> Não configurada</>}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Personal data */}
      <form onSubmit={handleProfileSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
        <div className="flex items-center gap-2 mb-1">
          <UserRound size={16} className="text-indigo-400" />
          <h3 className="text-white text-sm font-semibold">Dados pessoais</h3>
        </div>

        <div>
          <label className={labelCls}>Nome completo</label>
          <input
            value={fullName}
            onChange={e => setFullName(e.target.value)}
            required
            minLength={3}
            maxLength={255}
            className={inputCls}
          />
        </div>

        <div>
          <label className={labelCls}>Telefone</label>
          <input
            type="tel"
            value={phone}
            onChange={e => setPhone(formatPhone(e.target.value))}
            placeholder="(11) 99999-9999"
            className={inputCls}
          />
        </div>

        <div>
          <label className={labelCls}>E-mail</label>
          <input value={profile.email} disabled className={`${inputCls} opacity-60 cursor-not-allowed`} />
          <p className="text-gray-600 text-xs mt-1.5">O e-mail é usado para login e não pode ser alterado.</p>
        </div>

        <button
          type="submit"
          disabled={profileMutation.isPending}
          className="flex items-center justify-center gap-2 bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold px-5 py-2.5 rounded-xl transition-colors"
        >
          <Save size={15} />
          {profileMutation.isPending ? 'Salvando...' : 'Salvar alterações'}
        </button>
      </form>

      {/* Security */}
      <div className="grid lg:grid-cols-2 gap-6">
        {/* Password */}
        <form onSubmit={handlePasswordSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
          <div className="flex items-center gap-2 mb-1">
            <Lock size={16} className="text-indigo-400" />
            <h3 className="text-white text-sm font-semibold">Alterar senha</h3>
          </div>

          <PasswordField
            label="Senha atual"
            value={currentPassword}
            onChange={setCurrentPassword}
            placeholder="••••••••"
            autoComplete="current-password"
          />
          <PasswordField
            label="Nova senha"
            value={newPassword}
            onChange={setNewPassword}
            placeholder="Mínimo 8 caracteres"
            autoComplete="new-password"
          />

          {newPassword.length > 0 && (
            <ul className="space-y-1">
              {rules.map(rule => (
                <li key={rule.label} className={`flex items-center gap-1.5 text-xs ${rule.ok ? 'text-green-400' : 'text-gray-600'}`}>
                  <CheckCircle2 size={12} className={rule.ok ? 'opacity-100' : 'opacity-30'} />
                  {rule.label}
                </li>
              ))}
            </ul>
          )}

          <PasswordField
            label="Confirmar nova senha"
            value={confirmPassword}
            onChange={setConfirmPassword}
            placeholder="Repita a nova senha"
            autoComplete="new-password"
          />

          {confirmPassword.length > 0 && confirmPassword !== newPassword && (
            <p className="text-red-400 text-xs">As senhas não coincidem.</p>
          )}

          <button
            type="submit"
            disabled={passwordMutation.isPending}
            className="w-full bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-xl transition-colors"
          >
            {passwordMutation.isPending ? 'Alterando...' : 'Alterar senha'}
          </button>
        </form>

        {/* Transaction PIN */}
        <form onSubmit={handlePinSubmit} className="bg-[#12121a] border border-[#1e1e2e] rounded-2xl p-6 space-y-4">
          <div className="flex items-center gap-2 mb-1">
            <KeyRound size={16} className="text-indigo-400" />
            <h3 className="text-white text-sm font-semibold">Senha de transação</h3>
          </div>

          <p className="text-gray-600 text-xs -mt-2">
            Usada para autorizar transferências PIX. Nunca compartilhe este PIN.
          </p>

          <PasswordField
            label="Senha de acesso"
            value={pinPassword}
            onChange={setPinPassword}
            placeholder="Sua senha de login"
            autoComplete="current-password"
          />

          <div>
            <label className={labelCls}>Novo PIN (4 dígitos)</label>
            <input
              type="password"
              inputMode="numeric"
              value={newPin}
              onChange={e => setNewPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
              required
              pattern="[0-9]{4}"
              maxLength={4}
              placeholder="••••"
              className={inputCls}
            />
          </div>

          <div>
            <label className={labelCls}>Confirmar novo PIN</label>
            <input
              type="password"
              inputMode="numeric"
              value={confirmPin}
              onChange={e => setConfirmPin(e.target.value.replace(/\D/g, '').slice(0, 4))}
              required
              pattern="[0-9]{4}"
              maxLength={4}
              placeholder="••••"
              className={inputCls}
            />
          </div>

          {confirmPin.length === 4 && confirmPin !== newPin && (
            <p className="text-red-400 text-xs">Os PINs não coincidem.</p>
          )}

          <button
            type="submit"
            disabled={pinMutation.isPending}
            className="w-full bg-indigo-500 hover:bg-indigo-600 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold py-2.5 rounded-xl transition-colors"
          >
            {pinMutation.isPending ? 'Alterando...' : 'Alterar PIN'}
          </button>
        </form>
      </div>

      <p className="text-gray-600 text-xs text-center">
        Sessão de {user?.email ?? profile.email} · mantida neste dispositivo
      </p>
    </div>
  );
}
