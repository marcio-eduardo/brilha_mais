import React, { useEffect, useState } from 'react';
import { Medal, Trophy, Star } from 'lucide-react';
import { api } from '../services/api';
import { useAuthStore } from '../store/authStore';

export default function RankingScreen() {
  const { user } = useAuthStore();
  const [rankingData, setRankingData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [myPos, setMyPos] = useState<number | string>('--');

  useEffect(() => {
    let mounted = true;
    
    const fetchRanking = async () => {
      try {
        const response = await api.get('/dashboard/ranking');
        if (mounted) {
          const normalize = (str: string) => str ? str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim() : '';
          
          let myPosition: number | string = '--';
          const mappedData = response.data.map((r: any) => {
            const isMe = (user?.nomeCompleto && r.tecnico && normalize(r.tecnico) === normalize(user.nomeCompleto)) ||
                         (user?.matricula && r.matricula && String(r.matricula) === String(user.matricula));
            
            if (isMe) myPosition = r.posicaoRanking;
            
            return {
              id: r.matricula || r.tecnico || Math.random().toString(),
              name: r.tecnico,
              score: r.pontosTotal,
              base: '', // Base ATP não está disponível no provisório
              isMe: isMe,
              posicaoRanking: r.posicaoRanking
            };
          });
          
          setRankingData(mappedData);
          setMyPos(myPosition);
        }
      } catch (error) {
        console.error('Erro ao buscar ranking:', error);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    
    fetchRanking();
    
    return () => { mounted = false; };
  }, [user]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent-teal"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-6">
      <div className="bg-light-surface p-6 rounded-positivo-lg shadow-sm border border-light-border flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-light-text-main flex items-center">
            <Trophy className="text-brilhamais-gold mr-2" size={24} />
            Ranking Geral
          </h2>
          <p className="text-sm text-light-text-muted mt-1">Sua posição atual é a {myPos}ª</p>
        </div>
        <div className="bg-positivo-primary text-white w-14 h-14 rounded-full flex items-center justify-center font-bold text-2xl shadow-md border-4 border-light-background">
          {myPos}º
        </div>
      </div>

      <div className="bg-light-surface rounded-positivo-lg shadow-sm border border-light-border overflow-hidden">
        <ul className="divide-y divide-light-border">
          {rankingData.map((usr, index) => (
            <li 
              key={usr.id + '-' + index} 
              className={`p-4 flex items-center justify-between ${
                usr.isMe ? 'bg-amber-50/50 relative' : ''
              }`}
            >
              {usr.isMe && (
                <div className="absolute left-0 top-0 bottom-0 w-1 bg-brilhamais-gold"></div>
              )}
              
              <div className="flex items-center space-x-4">
                <div className="font-bold text-light-text-muted w-6 text-center">
                  {usr.posicaoRanking}
                </div>
                
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                  index === 0 ? 'bg-yellow-100 text-yellow-600' :
                  index === 1 ? 'bg-light-borderStrong text-light-text-muted' :
                  index === 2 && !usr.isMe ? 'bg-orange-100 text-orange-600' :
                  'bg-positivo-secondary text-white'
                }`}>
                  {index < 3 ? <Medal size={20} /> : <Star size={16} />}
                </div>
                
                <div>
                  <p className={`font-semibold ${usr.isMe ? 'text-light-text-main' : 'text-light-text-secondary'}`}>
                    {usr.name}
                  </p>
                  <p className="text-xs text-light-text-muted">{usr.base}</p>
                </div>
              </div>
              
              <div className="font-bold text-light-text-main">
                {typeof usr.score === 'number' ? usr.score.toFixed(1) : usr.score} <span className="text-xs text-light-text-muted font-normal">pts</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
