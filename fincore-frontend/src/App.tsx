import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute    from './components/PrivateRoute';
import Layout          from './components/Layout';
import Login           from './pages/Login';
import Register        from './pages/Register';
import Dashboard       from './pages/Dashboard';
import Pix             from './pages/Pix';
import Deposit         from './pages/Deposit';
import Statement       from './pages/Statement';
import NotFound        from './pages/NotFound';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Rotas públicas */}
          <Route path="/login"    element={<Login />}    />
          <Route path="/register" element={<Register />} />

          {/* Rotas protegidas */}
          <Route element={<PrivateRoute />}>
            <Route element={<Layout />}>
              <Route path="/"          element={<Dashboard />} />
              <Route path="/pix"       element={<Pix />}       />
              <Route path="/deposit"   element={<Deposit />}   />
              <Route path="/statement" element={<Statement />} />
            </Route>
          </Route>

          {/* 404 */}
          <Route path="/404" element={<NotFound />} />
          <Route path="*"    element={<Navigate to="/404" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
