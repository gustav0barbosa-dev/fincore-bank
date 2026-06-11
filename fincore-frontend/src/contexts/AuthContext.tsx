import { createContext, useContext, useState, ReactNode } from 'react';
import { User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  signIn: (token: string, user: User) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem('fincore_token')
  );
  const [user, setUser] = useState<User | null>(() => {
    try {
      const s = localStorage.getItem('fincore_user');
      return s ? (JSON.parse(s) as User) : null;
    } catch {
      return null;
    }
  });

  const signIn = (newToken: string, newUser: User) => {
    localStorage.setItem('fincore_token', newToken);
    localStorage.setItem('fincore_user', JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  };

  const signOut = () => {
    localStorage.removeItem('fincore_token');
    localStorage.removeItem('fincore_user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
};
