import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import App from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000 },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#111522',
            color: '#f4f6fc',
            border: '1px solid rgba(148, 163, 184, 0.16)',
            borderRadius: '14px',
            fontSize: '13px',
            boxShadow: '0 18px 48px rgba(0, 0, 0, 0.28)',
          },
          success: { iconTheme: { primary: '#37c995', secondary: '#111522' } },
          error: { iconTheme: { primary: '#ff7185', secondary: '#111522' } },
        }}
      />
    </QueryClientProvider>
  </React.StrictMode>
);
