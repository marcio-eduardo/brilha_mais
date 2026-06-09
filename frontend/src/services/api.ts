import axios from 'axios';
import * as SecureStore from '../utils/secureStore';

// Detecta o IP dinamicamente para Web
const getBaseURL = () => {
  if (typeof window !== 'undefined' && window.location) {
    return `http://${window.location.hostname}:8080/api/v1`;
  }
  return 'http://localhost:8080/api/v1';
};

export const api = axios.create({
  baseURL: getBaseURL(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para injetar o Token em cada requisição
api.interceptors.request.use(
  async (config) => {
    try {
      const token = await SecureStore.getItemAsync('brilhamais_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.error('Erro ao recuperar token do SecureStore', error);
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor genérico para logging de respostas
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.warn('Acesso não autorizado. Sessão possivelmente expirada.');
    }
    return Promise.reject(error);
  }
);
