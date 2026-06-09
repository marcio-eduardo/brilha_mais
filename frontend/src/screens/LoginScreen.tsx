import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { api } from '../services/api';
import { jwtDecode } from 'jwt-decode';

export default function LoginScreen() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const form = e.target as HTMLFormElement;
    const userIdInput = (form.elements.namedItem('userId') as HTMLInputElement).value;
    const passwordInput = (form.elements.namedItem('password') as HTMLInputElement).value;

    try {
      // Bate no Backend real (Spring Boot / Postgres)
      const response = await api.post('/auth/login', {
        matricula: userIdInput,
        senha: passwordInput
      });

      const { accessToken, primeiroAcesso, nome, cargo } = response.data;
      const decoded: any = jwtDecode(accessToken);

      // Armazena na Store o token e os dados exatos extraídos da API
      await setAuth(accessToken, {
        matricula: userIdInput,
        primeiroAcesso: primeiroAcesso,
        nomeCompleto: nome || decoded.nome || decoded.sub || userIdInput,
        cargo: cargo
      });

      setLoading(false);
      
      if (primeiroAcesso) {
        navigate('/onboarding');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setLoading(false);
      setError('ID ou Senha inválidos. Verifique suas credenciais.');
      console.error('Erro de login:', err);
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex flex-col items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 bg-white p-8 rounded-positivo-lg shadow-lg border border-slate-100">
        <div className="flex flex-col items-center">
          <img
            className="h-36 w-auto mb-6"
            src="/Logo/positivo.svg"
            alt="Positivo Brilha Mais"
          />
          <h2 className="text-center text-2xl font-bold tracking-tight text-slate-900">
            Acesso ao Sistema
          </h2>
          <p className="mt-2 text-center text-sm text-slate-500">
            Programa de Desempenho Técnico
          </p>
        </div>

        {error && (
          <div className="bg-red-50 text-red-600 p-3 rounded-md text-sm text-center font-medium shadow-sm">
            {error}
          </div>
        )}

        <form className="mt-8 space-y-6" onSubmit={handleLogin}>
          <div className="space-y-4">
            <div>
              <label htmlFor="userId" className="block text-sm font-medium text-slate-700">ID Técnico ou CPF</label>
              <input
                id="userId"
                name="userId"
                type="text"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-3 border border-slate-300 placeholder-slate-400 text-slate-900 rounded-positivo-sm focus:outline-none focus:ring-2 focus:ring-brilhamais-gold focus:border-transparent sm:text-sm"
                placeholder="Ex: 12345"
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-700">Senha</label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-3 border border-slate-300 placeholder-slate-400 text-slate-900 rounded-positivo-sm focus:outline-none focus:ring-2 focus:ring-brilhamais-gold focus:border-transparent sm:text-sm"
                placeholder="••••••••"
              />
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-3 px-4 border border-transparent text-sm font-medium rounded-positivo-md text-white bg-positivo-primary hover:bg-positivo-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brilhamais-gold transition-colors shadow-md disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? 'Autenticando...' : 'Entrar'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
