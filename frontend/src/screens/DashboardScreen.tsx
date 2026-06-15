import React, { useEffect, useState } from 'react';
import { Award, TrendingUp, AlertCircle, UserCheck, CheckCircle2, Medal, ChevronDown, ChevronUp, Cpu, XCircle, Trophy, X, HeartHandshake } from 'lucide-react';
import { PieChart, Pie, Cell, ResponsiveContainer } from 'recharts';
import { useAuthStore } from '../store/authStore';
import { api } from '../services/api';

import { CircularProgress } from '../components/ui/CircularProgress';
import { ChamadoItem } from '../components/dashboard/ChamadoItem';

export default function DashboardScreen() {
  const { user } = useAuthStore();

  const [metricas, setMetricas] = useState<any>(null);
  const [selectedMonth, setSelectedMonth] = useState<string>('Média Final');
  const [loading, setLoading] = useState(true);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [isElegivelModalOpen, setIsElegivelModalOpen] = useState(false);
  const [isInelegivelModalOpen, setIsInelegivelModalOpen] = useState(false);

  useEffect(() => {
    let mounted = true;

    const applyData = (data: any) => {
      const normalize = (str: string) => str ? str.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim() : '';
      
      const dadosTecnico = data.find((t: any) =>
        (user?.nomeCompleto && t.tecnico && normalize(t.tecnico) === normalize(user.nomeCompleto)) ||
        (user?.matricula && t.matricula && String(t.matricula) === String(user.matricula))
      );

      if (dadosTecnico) {
        setMetricas(dadosTecnico);
      } else {
        setMetricas({
          pontosTotal: 0,
          percentualSla: 0,
          percentualReincidencia: 0,
          posicaoRanking: '--',
          ultimosChamados: [],
          percentualEficienciaPecas: 0
        });
      }
    };

    const fetchMetricas = async () => {
      try {
        // 1. Carrega dados em cache para mostrar a tela rápido
        const response = await api.get('/dashboard/ranking');
        if (mounted) applyData(response.data);

        // 2. Dispara recálculo silencioso no background
        if (user?.matricula) {
          api.post(`/dashboard/calcular-tecnico?matricula=${user.matricula}`)
            .then(async () => {
              const freshResponse = await api.get('/dashboard/ranking');
              if (mounted) applyData(freshResponse.data);
            })
            .catch(e => console.error('Erro no recálculo em background:', e));
        }

      } catch (error) {
        console.error('Erro ao buscar métricas do BD:', error);
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchMetricas();
    
    return () => {
      mounted = false;
    };
  }, [user]);

  const displayMetricas = React.useMemo(() => {
    if (!metricas) return null;
    if (selectedMonth === 'Média Final') return metricas;
    const monthData = metricas.historico?.find((h: any) => h.mes === selectedMonth);
    if (!monthData) return metricas;
    return { ...metricas, ...monthData };
  }, [metricas, selectedMonth]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-[60vh]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent-teal"></div>
      </div>
    );
  }

  const percentualConsumo = displayMetricas?.percentualEficienciaPecas || 0;
  const percentualSla = displayMetricas?.percentualSla || 0;
  const percentualReincidencia = displayMetricas?.percentualReincidencia || 0;
  const pontuacaoTotal = displayMetricas?.pontosTotal || 0;
  const eficienciaData = [
    { name: 'Consumo', value: percentualConsumo },
    { name: 'Restante', value: Math.max(100 - percentualConsumo, 0) },
  ];

  const chamados = displayMetricas?.ultimosChamados || [];

  const getPremioInfo = (pontos: number) => {
    if (pontos >= 90) return { titulo: '1º Prêmio', valor: 'R$ 300,00' };
    if (pontos >= 80) return { titulo: '2º Prêmio', valor: 'R$ 200,00' };
    if (pontos >= 70) return { titulo: '3º Prêmio', valor: 'R$ 100,00' };
    return null;
  };
  const premioAtual = getPremioInfo(pontuacaoTotal);

  return (
    <div className="space-y-6 pb-6">

      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-light-text-main dark:text-text-main">Dashboard de Performance</h1>
          <p className="text-sm text-light-text-muted dark:text-text-muted mt-1">
            Acompanhe os seus indicadores e ranking em tempo real.
          </p>
        </div>
        <div className="flex flex-col items-end gap-2">
          {displayMetricas?.elegivel ? (
            <button 
              onClick={() => setIsElegivelModalOpen(true)}
              className="flex items-center space-x-2 bg-transparent border border-accent-emerald text-accent-emerald px-4 py-2 rounded-full font-medium text-sm hover:bg-accent-emerald/10 transition-colors">
              <CheckCircle2 size={16} />
              <span>Elegível para Premiação</span>
            </button>
          ) : (
            <button 
              onClick={() => setIsInelegivelModalOpen(true)}
              className="flex items-center space-x-2 bg-transparent border border-status-danger text-status-danger px-4 py-2 rounded-full font-medium text-sm hover:bg-status-danger/10 transition-colors">
              <XCircle size={16} />
              <span>Não Elegível</span>
            </button>
          )}
          {displayMetricas?.posicaoRanking && displayMetricas.posicaoRanking !== '--' && (
             <span className="text-xs font-bold text-light-text-muted bg-slate-100 dark:bg-slate-800 px-3 py-1 rounded-full flex items-center shadow-sm">
               <Medal size={12} className="mr-1 text-accent-teal" /> Ranking: {displayMetricas.posicaoRanking}º Lugar
             </span>
          )}
        </div>
      </div>

      {/* Seletor de Mês (Segmented Control) */}
      {metricas?.historico && metricas.historico.length > 0 && (
        <div className="flex justify-center mt-2 mb-8">
          <div className="inline-flex bg-slate-100 dark:bg-background/80 p-1.5 rounded-full border border-light-borderStrong dark:border-border/50 shadow-inner overflow-x-auto max-w-full scrollbar-hide">
            {['Média Final', ...metricas.historico.map((h: any) => h.mes).filter((m: string) => m !== 'Média Final')].map((monthOption: string) => (
              <button
                key={monthOption}
                onClick={() => setSelectedMonth(monthOption)}
                className={`px-6 py-2 rounded-full text-sm font-bold transition-all duration-300 whitespace-nowrap ${
                  selectedMonth === monthOption
                    ? 'bg-light-surface dark:bg-surface text-accent-teal shadow-md border border-light-borderStrong/50 dark:border-border transform scale-105'
                    : 'text-light-text-muted dark:text-light-text-muted hover:text-light-text-secondary dark:hover:text-slate-200 hover:bg-slate-200/50 dark:hover:bg-surface/50'
                }`}
              >
                {monthOption === 'Média Final' ? 'Campanha Inteira' : monthOption}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Top Grid: Pontuação & Últimos Chamados */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        
        {/* Card Pontuação Total */}
        <div 
          onClick={() => setDetailsModalOpen(true)}
          className="bg-gradient-to-br from-slate-900 to-slate-800 dark:from-slate-800 dark:to-background rounded-positivo-lg p-6 border border-slate-700 shadow-xl flex flex-col items-center justify-center text-center relative overflow-hidden group cursor-pointer hover:border-accent-teal/50 hover:shadow-2xl hover:shadow-accent-teal/20 transition-all duration-300"
        >
          <div className="absolute -right-6 -top-6 text-light-text-secondary/30 dark:text-light-text-secondary/20 transform group-hover:scale-110 transition-transform duration-500">
            <Award size={120} />
          </div>
          <h3 className="text-sm font-medium text-slate-300 mb-2 z-10 uppercase tracking-widest">Pontuação Total</h3>
          <div className="flex items-baseline gap-1 z-10">
            <span className="text-6xl font-black text-white">{pontuacaoTotal}</span>
            <span className="text-lg text-light-text-muted font-bold">/100</span>
          </div>
          <div className="mt-4 bg-light-surface/10 backdrop-blur px-4 py-1.5 rounded-full z-10">
             <p className="text-xs text-slate-200 font-medium flex items-center">
               <TrendingUp size={14} className="mr-1 text-accent-emerald" /> 
               Sua performance global
             </p>
          </div>
        </div>

        {/* Últimos Chamados */}
        <div className="lg:col-span-2 bg-light-surface dark:bg-surface p-6 rounded-positivo-lg shadow-sm border border-light-border dark:border-border">
          <h3 className="text-base font-bold text-light-text-main dark:text-text-main mb-4">Últimos Chamados Apurados</h3>
          <div className="space-y-3">
            {chamados.map((item: any) => (
              <ChamadoItem key={item.id} item={item} />
            ))}
          </div>
        </div>
      </div>

      {/* Grid Inferior: 6 KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        
        {/* Card SLA (Equipe) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-accent-teal/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-light-text-muted px-2 py-0.5 rounded-full mb-1">EQUIPE</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">SLA On-site</h3>
          </div>
          <CircularProgress
            value={percentualSla}
            maxValue={100}
            color="#00d8a6"
            label={percentualSla.toFixed(1)}
            isPercentage={true}
          />
          <p className="text-[10px] text-light-text-muted mt-1">Meta: ≥ 90%</p>
        </div>

        {/* Card Reincidência (Equipe) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-status-danger/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-light-text-muted px-2 py-0.5 rounded-full mb-1">EQUIPE</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">Reincidência</h3>
          </div>
          <CircularProgress
            value={displayMetricas?.percentualReincidenciaEquipe || 0}
            maxValue={100}
            color="#EF4444"
            label={(displayMetricas?.percentualReincidenciaEquipe || 0).toFixed(1)}
            isPercentage={true}
          />
          <p className="text-[10px] text-light-text-muted mt-1">Meta: {'<'} 7%</p>
        </div>

        {/* Card Perdas SLA (Equipe) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-orange-400/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-light-text-muted px-2 py-0.5 rounded-full mb-1">EQUIPE</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">Perdas de SLA</h3>
          </div>
          <CircularProgress
            value={displayMetricas?.percentualPerdidos || 0}
            maxValue={100}
            color="#fb923c"
            label={(displayMetricas?.percentualPerdidos || 0).toFixed(1)}
            isPercentage={true}
          />
          <p className="text-[10px] text-light-text-muted mt-1">Meta: ≤ 1%</p>
        </div>

        {/* Card NPS (Equipe) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-accent-blue/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-light-text-muted px-2 py-0.5 rounded-full mb-1">EQUIPE</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">Satisfação (NPS)</h3>
          </div>
          <CircularProgress
            value={displayMetricas?.npsScore || 0}
            maxValue={100}
            color="#3b82f6"
            label={(displayMetricas?.npsScore || 0).toFixed(0)}
            isPercentage={true}
          />
          <p className="text-[10px] text-light-text-muted mt-1">Gatilho Ouro</p>
        </div>

        {/* Card Reincidência Técnica (Individual) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-pink-500/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-accent-teal/10 text-accent-teal px-2 py-0.5 rounded-full mb-1">INDIVIDUAL</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">Reincidência Técnica</h3>
          </div>
          <CircularProgress
            value={percentualReincidencia}
            maxValue={100}
            color="#ec4899"
            label={percentualReincidencia.toFixed(1)}
            isPercentage={true}
          />
          <p className="text-[10px] text-light-text-muted mt-1">Meta: {'<'} 7%</p>
        </div>

        {/* Card Uso de Peças (Individual) */}
        <div className="bg-light-surface dark:bg-surface rounded-positivo-lg p-4 border border-light-border dark:border-border shadow-sm flex flex-col items-center text-center justify-center hover:border-cyan-400/30 transition-colors">
          <div className="flex flex-col items-center mb-2">
            <span className="text-[10px] font-bold bg-accent-teal/10 text-accent-teal px-2 py-0.5 rounded-full mb-1">INDIVIDUAL</span>
            <h3 className="text-xs font-bold text-light-text-secondary dark:text-text-main uppercase tracking-wider">Uso de Peças</h3>
          </div>
          
          <div className="relative w-36 h-36 mx-auto mb-2 mt-2">
            {percentualConsumo === 0 ? (
               <div className="absolute inset-0 flex flex-col items-center justify-center bg-accent-teal/10 rounded-full border border-accent-teal/20">
                 <CheckCircle2 size={32} className="text-accent-teal mb-1" />
                 <span className="text-xl font-bold text-accent-teal">0%</span>
               </div>
            ) : (
               <>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={eficienciaData}
                      cx="50%"
                      cy="50%"
                      innerRadius={46}
                      outerRadius={56}
                      startAngle={90}
                      endAngle={-270}
                      dataKey="value"
                      stroke="none"
                    >
                      <Cell key="consumo" fill="#00E5FF" />
                      <Cell key="restante" fill="#f1f5f9" className="dark:fill-slate-800" />
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>

                {/* Texto no centro do Donut */}
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-2xl font-bold text-light-text-main dark:text-text-main">{percentualConsumo.toFixed(1)}%</span>
                </div>
               </>
            )}
          </div>
          <p className="text-[10px] text-light-text-muted mt-1">Meta: ≤ 25%</p>
        </div>

      </div>

      {/* Modal de Detalhes da Pontuação */}
      {detailsModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 animate-in fade-in">
          <div className="bg-light-surface dark:bg-surface rounded-xl shadow-2xl w-full max-w-4xl overflow-hidden border border-light-borderStrong dark:border-border animate-in zoom-in-95">
            <div className="p-6 border-b border-light-borderStrong dark:border-border flex justify-between items-center bg-light-background dark:bg-background/50">
              <h2 className="text-xl font-bold text-light-text-main dark:text-text-main flex items-center gap-2">
                <Award className="text-accent-teal" /> Detalhamento da Pontuação
              </h2>
              <button onClick={() => setDetailsModalOpen(false)} className="text-light-text-muted hover:text-slate-600 transition-colors">
                <XCircle size={24} />
              </button>
            </div>
            <div className="p-6 overflow-x-auto overflow-y-auto max-h-[70vh] scrollbar-hide">
              <table className="w-full text-left border-collapse min-w-[800px]">
                <thead>
                  <tr className="bg-slate-100 dark:bg-background text-slate-600 dark:text-text-muted text-sm font-bold uppercase tracking-wider">
                    <th className="p-4 border-b border-light-borderStrong dark:border-border rounded-tl-lg">Mês</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center">SLA</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center" title="Reincidência Equipe">Reinc. (Eqp)</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center" title="Reincidência Individual">Reinc. (Ind)</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center">Perdas</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center">NPS</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center">Peças</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center">Total</th>
                    <th className="p-4 border-b border-light-borderStrong dark:border-border text-center rounded-tr-lg">Elegibilidade</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-border/50 bg-light-surface dark:bg-surface">
                  {metricas?.historico?.map((h: any, index: number) => {
                    const isMedia = h.mes === 'Média Final';
                    return (
                      <tr key={index} className={`hover:bg-light-background dark:hover:bg-background/50 transition-colors ${isMedia ? 'bg-light-background dark:bg-background/30' : ''}`}>
                        <td className="p-4 font-bold text-light-text-main dark:text-text-main flex items-center gap-2">
                          {isMedia ? <TrendingUp size={16} className="text-accent-teal"/> : null}
                          {h.mes}
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.percentualSla?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosSla} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.percentualReincidenciaEquipe?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosReincidenciaEquipe} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.percentualReincidencia?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosReincidencia} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.percentualPerdidos?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosPerdidos} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.npsScore?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosNps} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="font-bold text-light-text-secondary dark:text-text-main">{h.percentualEficienciaPecas?.toFixed(2)}%</div>
                          <div className="text-xs font-medium text-accent-teal bg-accent-teal/10 px-2 py-0.5 rounded-full inline-block mt-1 whitespace-nowrap">{h.pontosPecas} pts</div>
                        </td>
                        <td className="p-4 text-center">
                          <div className="text-2xl font-black text-light-text-main dark:text-text-main">{h.pontosTotal}</div>
                          <div className="text-[10px] text-light-text-muted font-bold uppercase tracking-widest mt-1">Pontos</div>
                        </td>
                        <td className="p-4 text-center">
                          {h.elegivel ? (
                            <span className="bg-accent-emerald text-white text-xs font-bold px-3 py-1.5 rounded-full inline-flex items-center gap-1 shadow-sm"><CheckCircle2 size={14}/> Elegível</span>
                          ) : (
                            <span className="bg-status-danger text-white text-xs font-bold px-3 py-1.5 rounded-full inline-flex items-center gap-1 shadow-sm" title={h.motivoInelegibilidade}><XCircle size={14}/> Inelegível</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            <div className="p-4 bg-light-background dark:bg-background/80 border-t border-light-borderStrong dark:border-border flex justify-end">
              <button onClick={() => setDetailsModalOpen(false)} className="bg-slate-800 hover:bg-slate-900 text-white px-6 py-2 rounded-lg font-medium transition-colors shadow-md hover:shadow-lg focus:ring-2 focus:ring-slate-400 focus:outline-none">
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Elegível */}
      {isElegivelModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in">
          <div className="bg-light-surface dark:bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-accent-emerald/30 animate-in zoom-in-95 text-center relative p-8">
            <button onClick={() => setIsElegivelModalOpen(false)} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors">
              <X size={24} />
            </button>
            <div className="mx-auto bg-accent-emerald/10 w-24 h-24 rounded-full flex items-center justify-center mb-6">
              <Trophy size={48} className="text-accent-emerald" />
            </div>
            <h2 className="text-3xl font-black text-light-text-main dark:text-text-main mb-2">
              {premioAtual ? "Parabéns!" : "Quase lá!"}
            </h2>
            <p className="text-light-text-secondary dark:text-slate-300 mb-6 text-lg">
              {premioAtual 
                ? "Você está elegível para a premiação neste período. Continue se empenhando para manter ou melhorar seus resultados!"
                : "Você manteve seus indicadores dentro da meta de elegibilidade, mas a sua pontuação total ainda não atingiu a faixa de premiação."}
            </p>
            
            <div className="bg-slate-50 dark:bg-slate-800/50 rounded-2xl p-6 border border-slate-100 dark:border-slate-700/50">
              <p className="text-sm font-bold text-light-text-muted dark:text-slate-400 uppercase tracking-widest mb-2">Prêmio Projetado</p>
              {premioAtual ? (
                <>
                  <div className="text-xl font-bold text-accent-emerald">{premioAtual.titulo}</div>
                  <div className="text-4xl font-black text-light-text-main dark:text-white mt-1">{premioAtual.valor}</div>
                  <p className="text-xs text-light-text-muted dark:text-slate-500 mt-3">* Baseado na sua pontuação de {pontuacaoTotal} pontos.</p>
                </>
              ) : (
                <>
                  <div className="text-2xl font-black text-slate-500 dark:text-slate-400 mb-2">{pontuacaoTotal} pontos</div>
                  <p className="text-sm font-medium text-light-text-main dark:text-slate-300 leading-relaxed">
                    Não desanime, sua pontuação pode melhorar! Lembre-se que a primeira faixa de premiação começa a partir de <strong>70 pontos</strong>.
                  </p>
                </>
              )}
            </div>

            <button onClick={() => setIsElegivelModalOpen(false)} className="mt-8 w-full bg-accent-emerald hover:bg-emerald-600 text-white py-4 rounded-xl font-bold text-lg transition-colors shadow-lg shadow-accent-emerald/20">
              Incrível!
            </button>
          </div>
        </div>
      )}

      {/* Modal Inelegível */}
      {isInelegivelModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-in fade-in">
          <div className="bg-light-surface dark:bg-surface rounded-3xl shadow-2xl w-full max-w-md overflow-hidden border border-status-danger/30 animate-in zoom-in-95 text-center relative p-8">
            <button onClick={() => setIsInelegivelModalOpen(false)} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors">
              <X size={24} />
            </button>
            <div className="mx-auto bg-status-danger/10 w-24 h-24 rounded-full flex items-center justify-center mb-6">
              <HeartHandshake size={48} className="text-status-danger" />
            </div>
            <h2 className="text-3xl font-black text-light-text-main dark:text-text-main mb-2">Não desanime!</h2>
            <p className="text-light-text-secondary dark:text-slate-300 mb-6 text-lg">
              Infelizmente, você não atingiu a elegibilidade neste período. Mas o próximo mês é uma nova chance de brilhar!
            </p>
            
            <div className="bg-red-50 dark:bg-red-950/20 rounded-2xl p-6 border border-red-100 dark:border-red-900/30">
              <p className="text-sm font-bold text-status-danger uppercase tracking-widest mb-2 flex items-center justify-center gap-2">
                <AlertCircle size={16} /> Motivo
              </p>
              <div className="text-base font-medium text-light-text-main dark:text-slate-200">
                {displayMetricas?.motivoInelegibilidade || "Pontuação ou gatilho não atingido."}
              </div>
            </div>

            <button onClick={() => setIsInelegivelModalOpen(false)} className="mt-8 w-full bg-slate-800 hover:bg-slate-900 text-white py-4 rounded-xl font-bold text-lg transition-colors shadow-lg">
              Vou melhorar!
            </button>
          </div>
        </div>
      )}

    </div>
  );
}
