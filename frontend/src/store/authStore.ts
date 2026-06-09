import { create } from 'zustand';
import * as SecureStore from '../utils/secureStore';

export interface UserProfile {
  matricula: string;
  primeiroAcesso: boolean;
  nomeCompleto?: string;
  cargo?: string;
}

interface AuthState {
  token: string | null;
  user: UserProfile | null;
  isLoading: boolean;
  setAuth: (token: string, user: UserProfile) => Promise<void>;
  updateUser: (userUpdates: Partial<UserProfile>) => Promise<void>;
  logout: () => Promise<void>;
  checkSession: () => Promise<void>;
  hydrate: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  token: null,
  user: null,
  isLoading: true, // Começa carregando para evitar piscar a tela de login se já estiver logado

  setAuth: async (token: string, user: UserProfile) => {
    try {
      await SecureStore.setItemAsync('brilhamais_token', token);
      await SecureStore.setItemAsync('brilhamais_user', JSON.stringify(user));
      set({ token, user, isLoading: false });
    } catch (error) {
      console.error('Erro ao salvar no SecureStore:', error);
    }
  },

  updateUser: async (userUpdates: Partial<UserProfile>) => {
    try {
      const currentUser = get().user;
      if (!currentUser) return;
      const updatedUser = { ...currentUser, ...userUpdates };
      await SecureStore.setItemAsync('brilhamais_user', JSON.stringify(updatedUser));
      set({ user: updatedUser });
    } catch (error) {
      console.error('Erro ao atualizar usuário no SecureStore:', error);
    }
  },

  logout: async () => {
    try {
      await SecureStore.deleteItemAsync('brilhamais_token');
      await SecureStore.deleteItemAsync('brilhamais_user');
      set({ token: null, user: null, isLoading: false });
    } catch (error) {
      console.error('Erro ao deletar do SecureStore:', error);
    }
  },

  checkSession: async () => {
    try {
      await SecureStore.deleteItemAsync('brilhamais_token');
      await SecureStore.deleteItemAsync('brilhamais_user');
      set({ token: null, user: null, isLoading: false });
    } catch (error) {
      console.error('Erro ao verificar/limpar sessão:', error);
      set({ token: null, user: null, isLoading: false });
    }
  },

  hydrate: async () => {
    try {
      const token = await SecureStore.getItemAsync('brilhamais_token');
      const userStr = await SecureStore.getItemAsync('brilhamais_user');
      if (token && userStr) {
        set({ token, user: JSON.parse(userStr), isLoading: false });
      } else {
        set({ token: null, user: null, isLoading: false });
      }
    } catch (error) {
      set({ token: null, user: null, isLoading: false });
    }
  },
}));
