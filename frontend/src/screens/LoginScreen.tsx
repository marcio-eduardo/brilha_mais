import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { api } from '../services/api';
import { jwtDecode } from 'jwt-decode';
import { User, ShieldCheck } from 'lucide-react';

export default function LoginScreen() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Estados do Modal de Primeiro Acesso
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [primeiroAcessoStep, setPrimeiroAcessoStep] = useState<1 | 2>(1);
  const [paNome, setPaNome] = useState('');
  const [paEstado, setPaEstado] = useState('');
  const [paMatricula, setPaMatricula] = useState('');
  const [paLoading, setPaLoading] = useState(false);
  const [paError, setPaError] = useState('');
  const [tecnicoId, setTecnicoId] = useState<number | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const form = e.target as HTMLFormElement;
    const userIdInput = (form.elements.namedItem('userId') as HTMLInputElement).value;
    const passwordInput = (form.elements.namedItem('password') as HTMLInputElement).value;

    try {
      const response = await api.post('/auth/login', {
        matricula: userIdInput,
        senha: passwordInput
      });

      const { accessToken, primeiroAcesso, nome, cargo } = response.data;
      const decoded: any = jwtDecode(accessToken);

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

  const handleVerificarTecnico = async (e: React.FormEvent) => {
    e.preventDefault();
    setPaError('');

    if (paMatricula.length !== 5) {
      setPaError('Matrícula inválida');
      return;
    }

    setPaLoading(true);
    try {
      const response = await api.post('/auth/verificar-tecnico', {
        nome: paNome,
        estado: paEstado
      });
      setTecnicoId(response.data.id);
      setPrimeiroAcessoStep(2);
    } catch (err: any) {
      setPaError('O Nome ou Estado divergente. Procure seu gestor.');
    } finally {
      setPaLoading(false);
    }
  };

  const handleConfirmarMatricula = async () => {
    setPaError('');
    setPaLoading(true);
    try {
      const response = await api.post('/auth/vincular-matricula', {
        id: tecnicoId,
        matricula: paMatricula
      });

      const { accessToken, primeiroAcesso, nome, cargo } = response.data;
      const decoded: any = jwtDecode(accessToken);

      await setAuth(accessToken, {
        matricula: paMatricula,
        primeiroAcesso: primeiroAcesso,
        nomeCompleto: nome || decoded.nome || decoded.sub || paNome,
        cargo: cargo
      });

      setIsModalOpen(false);
      navigate('/onboarding');
    } catch (err: any) {
      setPaError('Erro ao vincular matrícula. Tente novamente.');
    } finally {
      setPaLoading(false);
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
            width={120}
            height={144}
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
              <label htmlFor="userId" className="block text-sm font-medium text-slate-700">Matrícula</label>
              <input
                id="userId"
                name="userId"
                type="text"
                required
                autoComplete="username"
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
                autoComplete="current-password"
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

          <div className="mt-4 text-center">
            <button
              type="button"
              onClick={() => {
                setIsModalOpen(true);
                setPrimeiroAcessoStep(1);
                setPaError('');
                setPaNome('');
                setPaEstado('');
                setPaMatricula('');
              }}
              className="text-sm font-medium text-positivo-primary hover:text-positivo-secondary transition-colors"
            >
              Primeiro acesso? Cadastre-se aqui
            </button>
          </div>
        </form>
      </div>

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/80 backdrop-blur-md">
          <div className="w-full max-w-md z-10 animate-in fade-in zoom-in-95 duration-200">
            <div className="bg-[#1e293b] backdrop-blur-xl border border-white/10 p-8 rounded-3xl shadow-2xl">
              
              <div className="flex items-center justify-center mb-6">
                <div className="w-16 h-16 bg-accent-teal/10 text-accent-teal rounded-full flex items-center justify-center shadow-lg shadow-accent-teal/20">
                  {primeiroAcessoStep === 1 ? <User size={32} /> : <ShieldCheck size={32} />}
                </div>
              </div>

              <h3 className="text-2xl font-bold text-center text-white mb-2">Primeiro Acesso</h3>
              <p className="text-slate-400 text-center mb-8 text-sm">
                {primeiroAcessoStep === 1 
                  ? "Para iniciar seu cadastro, preencha seus dados de identificação abaixo." 
                  : "Quase lá! Confirme se a sua matrícula está correta."}
              </p>

              {paError && (
                <div className="bg-red-500/10 border border-red-500/50 text-red-200 p-3 rounded-xl text-center mb-6 text-sm">
                  {paError}
                </div>
              )}

              {primeiroAcessoStep === 1 ? (
                <form onSubmit={handleVerificarTecnico} className="space-y-4">
                  <div>
                    <input
                      id="paNome"
                      type="text"
                      required
                      value={paNome}
                      onChange={e => setPaNome(e.target.value)}
                      className="w-full bg-slate-900/50 border border-white/10 text-white rounded-xl px-4 py-4 focus:outline-none focus:ring-2 focus:ring-accent-teal focus:border-transparent transition-colors placeholder:text-slate-500"
                      placeholder="Nome Completo"
                    />
                  </div>
                  <div>
                    <input
                      id="paEstado"
                      type="text"
                      required
                      value={paEstado}
                      onChange={e => setPaEstado(e.target.value)}
                      className="w-full bg-slate-900/50 border border-white/10 text-white rounded-xl px-4 py-4 focus:outline-none focus:ring-2 focus:ring-accent-teal focus:border-transparent transition-colors placeholder:text-slate-500"
                      placeholder="Estado (Ex: RJ)"
                    />
                  </div>
                  <div>
                    <input
                      id="paMatricula"
                      type="text"
                      required
                      value={paMatricula}
                      onChange={e => setPaMatricula(e.target.value)}
                      className="w-full bg-slate-900/50 border border-white/10 text-white rounded-xl px-4 py-4 focus:outline-none focus:ring-2 focus:ring-accent-teal focus:border-transparent transition-colors placeholder:text-slate-500"
                      placeholder="Matrícula (Exatamente 5 dígitos)"
                    />
                  </div>
                  
                  <div className="flex gap-3 mt-6 pt-2">
                    <button
                      type="button"
                      onClick={() => setIsModalOpen(false)}
                      className="flex-1 px-4 py-4 border border-white/10 bg-white/5 text-slate-300 rounded-xl hover:bg-white/10 transition-colors font-bold text-sm"
                    >
                      Cancelar
                    </button>
                    <button
                      type="submit"
                      disabled={paLoading}
                      className="flex-1 px-4 py-4 bg-accent-teal hover:bg-emerald-400 text-slate-900 rounded-xl transition-colors font-bold text-sm disabled:opacity-50 hover:shadow-[0_0_20px_rgba(0,216,166,0.3)]"
                    >
                      {paLoading ? 'Verificando...' : 'Continuar'}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="space-y-6 text-center">
                  <div className="bg-slate-900/50 border border-white/5 rounded-2xl p-6">
                    <p className="text-slate-400 text-sm mb-2">Sua Matrícula</p>
                    <p className="text-3xl font-black text-white tracking-widest">{paMatricula}</p>
                  </div>
                  
                  <div className="flex gap-3 mt-8">
                    <button
                      type="button"
                      onClick={() => setPrimeiroAcessoStep(1)}
                      className="flex-1 px-4 py-4 border border-white/10 bg-white/5 text-slate-300 rounded-xl hover:bg-white/10 transition-colors font-bold text-sm"
                    >
                      Voltar
                    </button>
                    <button
                      type="button"
                      onClick={handleConfirmarMatricula}
                      disabled={paLoading}
                      className="flex-1 px-4 py-4 bg-accent-teal hover:bg-emerald-400 text-slate-900 rounded-xl transition-colors font-bold text-sm disabled:opacity-50 hover:shadow-[0_0_20px_rgba(0,216,166,0.3)]"
                    >
                      {paLoading ? 'Salvando...' : 'Confirmar e Salvar'}
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
