import { Routes, Route, Navigate } from 'react-router-dom';

// Layouts
import MainLayout from '../components/layout/MainLayout';

// Screens
import LoginScreen from '../screens/LoginScreen';
import DashboardScreen from '../screens/DashboardScreen';
import RankingScreen from '../screens/RankingScreen';
import ProfileScreen from '../screens/ProfileScreen';
import OnboardingScreen from '../screens/OnboardingScreen';
import { useAuthStore } from '../store/authStore';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { token, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen bg-light-background dark:bg-background">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent-teal"></div>
      </div>
    );
  }

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default function AppRoutes() {
  return (
    <Routes>
      {/* Rotas Públicas */}
      <Route path="/login" element={<LoginScreen />} />

      {/* Rota Privada sem Layout (Onboarding) */}
      <Route path="/onboarding" element={<ProtectedRoute><OnboardingScreen /></ProtectedRoute>} />

      {/* Rotas Privadas (Com o MainLayout) */}
      <Route element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
        <Route path="/dashboard" element={<DashboardScreen />} />
        <Route path="/ranking" element={<RankingScreen />} />
        <Route path="/profile" element={<ProfileScreen />} />
      </Route>

      {/* Redirecionamento padrão para rotas não encontradas */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
