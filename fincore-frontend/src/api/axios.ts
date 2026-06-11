import axios, { AxiosInstance } from 'axios';

// Os prefixos são reescritos pelo proxy Vite → backends reais
// /auth       → http://localhost:8081
// /account    → http://localhost:8082
// /transaction → http://localhost:8083
const createInstance = (baseURL: string): AxiosInstance => {
  const instance = axios.create({ baseURL, timeout: 15_000 });

  instance.interceptors.request.use((config) => {
    const token = localStorage.getItem('fincore_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  instance.interceptors.response.use(
    (res) => res,
    (err) => {
      if (err.response?.status === 401) {
        localStorage.removeItem('fincore_token');
        localStorage.removeItem('fincore_user');
        window.location.replace('/login');
      }
      return Promise.reject(err);
    }
  );

  return instance;
};

export const authApi        = createInstance('/auth');
export const accountApi     = createInstance('/account');
export const transactionApi = createInstance('/transaction');
