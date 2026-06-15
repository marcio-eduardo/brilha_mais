import { Link, useLocation } from 'react-router-dom';
import { LayoutDashboard, Trophy, User } from 'lucide-react';

export default function BottomNav() {
  const location = useLocation();

  const navItems = [
    { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/ranking', icon: Trophy, label: 'Ranking' },
    { path: '/profile', icon: User, label: 'Perfil' }
  ];

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 bg-light-surface dark:bg-background border-t border-light-borderStrong dark:border-border pb-safe shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-50">
      <div className="flex justify-around items-center h-16 max-w-md mx-auto px-4">
        {navItems.map((item) => {
          const isActive = location.pathname === item.path;
          const Icon = item.icon;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`${item.path === '/ranking' ? 'hidden' : 'flex'} flex-col items-center justify-center w-full h-full space-y-1 transition-colors ${
                isActive ? 'text-brilhamais-gold' : 'text-light-text-muted hover:text-slate-600'
              }`}
            >
              <Icon size={24} strokeWidth={isActive ? 2.5 : 2} />
              <span className={`text-[10px] font-medium ${isActive ? 'font-semibold' : ''}`}>
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
