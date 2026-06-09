/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./App.{js,ts,jsx,tsx}", // Adicionado para garantir compatibilidade com Expo/React Native
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // As tuas cores corporativas base
        positivo: {
          primary: '#0f172a', // slate-900 
          secondary: '#1e293b',
          accent: '#3b82f6', // blue-500
        },
        brilhamais: {
          gold: '#eab308', // yellow-500
          light: '#fef08a',
        },
        accent: {
          teal: '#00d8a6',    // Teal/Turquesa brilhante
          emerald: '#10b981', // Verde esmeralda para botões e variações
        },
        // Cores da Paleta Brilha Mais (Dark Mode & UI Gamificada - Neon/Cyber)
        background: '#0b1120', // Fundo principal escuro, tom azulado sutil
        surface: '#151e32',    // Fundo dos cartões/cards flutuantes
        border: '#1e293b',     // slate-800 - Linhas sutis e bordas

        primary: {
          DEFAULT: '#22d3ee',  // cyan-400 - Azul/Ciano Néon principal
          dark: '#0891b2',     // cyan-600 - Azul escuro para botões pressionados
          light: '#67e8f9',    // cyan-300 - Azul claro para gradientes
          transparent: 'rgba(34, 211, 238, 0.15)', // Fundo translúcido para ícones
        },

        text: {
          main: '#F8FAFC',     // Branco puro/azulado para títulos
          muted: '#94A3B8',    // Cinza ardósia para legendas
        },

        status: {
          success: '#10B981',  // emerald-500 - SLA Batido (Verde vibrante)
          warning: '#F59E0B',  // amber-500 - Atenção na meta (Laranja)
          danger: '#EF4444',   // red-500 - Meta Perdida (Vermelho vivo)
          info: '#3B82F6',     // blue-500 - Neutro (Azul)
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'positivo-lg': '16px',
        'positivo-md': '12px',
        'positivo-sm': '8px',
      }
    },
  },
  plugins: [],
}