import React from 'react';
import { Medal, Trophy, Star } from 'lucide-react';

const rankingData = [
  { id: 1, name: 'Marcos Silva', score: 98.5, base: 'Curitiba', isMe: false },
  { id: 2, name: 'João Santos', score: 95.2, base: 'São Paulo', isMe: false },
  { id: 3, name: 'Você', score: 92.5, base: 'Curitiba', isMe: true },
  { id: 4, name: 'Ana Oliveira', score: 89.0, base: 'Rio de Janeiro', isMe: false },
  { id: 5, name: 'Carlos Ferreira', score: 85.5, base: 'Belo Horizonte', isMe: false },
];

export default function RankingScreen() {
  return (
    <div className="space-y-6 pb-6">
      <div className="bg-white p-6 rounded-positivo-lg shadow-sm border border-slate-100 flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-900 flex items-center">
            <Trophy className="text-brilhamais-gold mr-2" size={24} />
            Ranking Geral
          </h2>
          <p className="text-sm text-slate-500 mt-1">Sua posição atual é a 3ª</p>
        </div>
        <div className="bg-positivo-primary text-white w-14 h-14 rounded-full flex items-center justify-center font-bold text-2xl shadow-md border-4 border-slate-50">
          3º
        </div>
      </div>

      <div className="bg-white rounded-positivo-lg shadow-sm border border-slate-100 overflow-hidden">
        <ul className="divide-y divide-slate-100">
          {rankingData.map((user, index) => (
            <li 
              key={user.id} 
              className={`p-4 flex items-center justify-between ${
                user.isMe ? 'bg-amber-50/50 relative' : ''
              }`}
            >
              {user.isMe && (
                <div className="absolute left-0 top-0 bottom-0 w-1 bg-brilhamais-gold"></div>
              )}
              
              <div className="flex items-center space-x-4">
                <div className="font-bold text-slate-400 w-6 text-center">
                  {index + 1}
                </div>
                
                <div className={`w-10 h-10 rounded-full flex items-center justify-center ${
                  index === 0 ? 'bg-yellow-100 text-yellow-600' :
                  index === 1 ? 'bg-slate-200 text-slate-600' :
                  index === 2 && !user.isMe ? 'bg-orange-100 text-orange-600' :
                  'bg-positivo-secondary text-white'
                }`}>
                  {index < 3 ? <Medal size={20} /> : <Star size={16} />}
                </div>
                
                <div>
                  <p className={`font-semibold ${user.isMe ? 'text-slate-900' : 'text-slate-700'}`}>
                    {user.name}
                  </p>
                  <p className="text-xs text-slate-500">{user.base}</p>
                </div>
              </div>
              
              <div className="font-bold text-slate-900">
                {user.score} <span className="text-xs text-slate-400 font-normal">pts</span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
