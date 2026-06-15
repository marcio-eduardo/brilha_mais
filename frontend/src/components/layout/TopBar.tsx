import { useAuthStore } from '../../store/authStore';
import { useThemeStore } from '../../store/themeStore';
import { Sun, Moon, Bell, User, Settings, LogOut } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import AdminSettingsModal from '../AdminSettingsModal';

export default function TopBar() {
  const { user, logout } = useAuthStore();
  const { theme, setTheme } = useThemeStore();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const toggleTheme = () => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  // Close menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const isAdmin = user?.cargo === 'Administrador' || user?.cargo === 'Admin' || user?.cargo === 'Super Administrador';

  return (
    <header className="fixed top-0 left-0 right-0 bg-light-surface dark:bg-background shadow-sm border-b border-light-borderStrong dark:border-border z-40 h-20 flex items-center justify-between px-6 pt-safe">

      {/* Esquerda: Logo */}
      <div className="flex items-center">
        <img src="/Logo/positivo.svg" alt="Positivo Brilha Mais" className="h-16 md:h-28 object-contain" />
      </div>

      {/* Meio: Navegação Desktop */}
      <nav className="hidden md:flex items-center space-x-8 absolute left-1/2 -translate-x-1/2">
        <Link to="/dashboard" className={`hidden text-sm font-semibold transition-colors ${location.pathname === '/dashboard' ? 'text-brilhamais-gold' : 'text-light-text-muted hover:text-light-text-secondary dark:text-text-muted dark:hover:text-text-main'}`}>Dashboard</Link>
        <Link to="/ranking" className={`hidden text-sm font-semibold transition-colors ${location.pathname === '/ranking' ? 'text-brilhamais-gold' : 'text-light-text-muted hover:text-light-text-secondary dark:text-text-muted dark:hover:text-text-main'}`}>Ranking</Link>
      </nav>

      {/* Direita: Ações e Perfil */}
      <div className="flex items-center space-x-4 md:space-x-6">

        {/* Toggle de Tema */}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-full text-light-text-muted hover:text-light-text-secondary dark:text-text-muted dark:hover:text-text-main transition-colors"
        >
          {theme === 'dark' ? <Sun size={20} /> : <Moon size={20} />}
        </button>

        {/* Notificações */}
        <button className="relative p-2 rounded-full text-light-text-muted hover:text-light-text-secondary dark:text-text-muted dark:hover:text-text-main transition-colors">
          <Bell size={20} />
          {/* Badge vermelho de notificação */}
          <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
        </button>

        {/* Separador */}
        <div className="hidden md:block w-px h-8 bg-slate-200 dark:bg-border"></div>

        {/* Perfil do Usuário com Dropdown */}
        <div className="relative" ref={menuRef}>
          <div 
            className="flex items-center space-x-3 cursor-pointer select-none"
            onClick={() => setIsMenuOpen(!isMenuOpen)}
          >
            <div className="hidden md:flex flex-col items-end">
              <span className="text-sm font-bold text-light-text-main dark:text-text-main leading-tight">
                {user?.nomeCompleto || 'Técnico'}
              </span>
              <span className="text-xs text-accent-teal font-medium">
                {user?.cargo || 'Técnico N2'}
              </span>
            </div>
            <div className="w-10 h-10 rounded-full bg-slate-100 dark:bg-surface border border-light-borderStrong dark:border-border flex items-center justify-center text-light-text-muted dark:text-text-muted hover:bg-slate-200 transition-colors">
              <User size={20} />
            </div>
          </div>

          {/* Menu Dropdown */}
          {isMenuOpen && (
            <div className="absolute right-0 mt-2 w-48 bg-light-surface dark:bg-surface rounded-md shadow-lg py-1 border border-light-borderStrong dark:border-border z-50 animate-in fade-in slide-in-from-top-2">
              {isAdmin && (
                <button
                  onClick={() => {
                    setIsMenuOpen(false);
                    setIsSettingsModalOpen(true);
                  }}
                  className="flex items-center w-full px-4 py-2 text-sm text-light-text-secondary dark:text-text-main hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                >
                  <Settings size={16} className="mr-2" />
                  Configurações
                </button>
              )}
              <button
                onClick={handleLogout}
                className="flex items-center w-full px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
              >
                <LogOut size={16} className="mr-2" />
                Sair
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Modal de Configurações */}
      <AdminSettingsModal 
        isOpen={isSettingsModalOpen} 
        onClose={() => setIsSettingsModalOpen(false)} 
      />
    </header>
  );
}
