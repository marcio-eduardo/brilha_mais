package br.com.positivo.brilhamais.services;

import br.com.positivo.brilhamais.models.ApuracaoMensal;
import br.com.positivo.brilhamais.models.Tecnico;
import br.com.positivo.brilhamais.models.Campanha;
import br.com.positivo.brilhamais.repositories.ApuracaoMensalRepository;
import br.com.positivo.brilhamais.repositories.TecnicoRepository;
import br.com.positivo.brilhamais.repositories.CampanhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MotorCalculoService {

    private final JdbcTemplate jdbcTemplate;
    private final TecnicoRepository tecnicoRepository;
    private final ApuracaoMensalRepository apuracaoRepository;
    private final CampanhaRepository campanhaRepository;

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?")
    public void rotinaDiariaCalculo() {
        calcularEProcessarMes(LocalDate.now().withDayOfMonth(1)); // Param is ignored, uses Campanha
    }

    @Transactional
    public void calcularEProcessarMes(LocalDate ignoredParam) {
        // Obter datas da campanha ativa
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        if (campanhaAtiva == null)
            return;

        LocalDate dataInicio = campanhaAtiva.getDataInicio();
        LocalDate dataFim = campanhaAtiva.getDataFim();

        List<Tecnico> tecnicos = tecnicoRepository.findAll();
        for (Tecnico tecnico : tecnicos) {
            if (!tecnico.getAtivo())
                continue;
            processarTecnico(tecnico, dataInicio, dataFim);
        }
    }

    @Transactional
    public void calcularEProcessarTecnico(String matricula) {
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        if (campanhaAtiva == null)
            return;

        Tecnico tecnico = tecnicoRepository.findByMatricula(matricula).orElse(null);
        if (tecnico == null || !tecnico.getAtivo())
            return;

        processarTecnico(tecnico, campanhaAtiva.getDataInicio(), campanhaAtiva.getDataFim());
    }

    
    private void processarTecnico(Tecnico tecnico, LocalDate dataInicioCampanha, LocalDate dataFimCampanha) {
        // Mês 1
        LocalDate m1Inicio = dataInicioCampanha.withDayOfMonth(1);
        LocalDate m1Fim = m1Inicio.withDayOfMonth(m1Inicio.lengthOfMonth());
        if (m1Fim.isAfter(dataFimCampanha)) m1Fim = dataFimCampanha;

        ApuracaoMensal ap1 = calcularParaPeriodo(tecnico, m1Inicio, m1Fim, m1Inicio);
        
        // Mês 2
        LocalDate m2Inicio = m1Fim.plusDays(1).withDayOfMonth(1);
        ApuracaoMensal ap2 = null;
        if (!m2Inicio.isAfter(dataFimCampanha)) {
            LocalDate m2Fim = m2Inicio.withDayOfMonth(m2Inicio.lengthOfMonth());
            if (m2Fim.isAfter(dataFimCampanha)) m2Fim = dataFimCampanha;
            ap2 = calcularParaPeriodo(tecnico, m2Inicio, m2Fim, m2Inicio);
        }

        // Média Geral
        if (ap2 != null) {
            ApuracaoMensal apFinal = apuracaoRepository
                .findFirstByTecnicoIdTecnicoAndMesAno(tecnico.getIdTecnico(), dataFimCampanha)
                .orElse(ApuracaoMensal.builder()
                        .tecnico(tecnico)
                        .mesAno(dataFimCampanha)
                        .build());
                        
            apFinal.setAtingimentoSla(ap1.getAtingimentoSla().add(ap2.getAtingimentoSla()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setPontosSla((ap1.getPontosSla() + ap2.getPontosSla()) / 2.0);
            
            apFinal.setAtingimentoReincidencia(ap1.getAtingimentoReincidencia().add(ap2.getAtingimentoReincidencia()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setPontosReincidencia((ap1.getPontosReincidencia() + ap2.getPontosReincidencia()) / 2.0);
            
            apFinal.setAtingimentoPecas(ap1.getAtingimentoPecas().add(ap2.getAtingimentoPecas()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setPontosPecas((ap1.getPontosPecas() + ap2.getPontosPecas()) / 2.0);
            
            apFinal.setAtingimentoNps(ap1.getAtingimentoNps().add(ap2.getAtingimentoNps()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setPontosNps((ap1.getPontosNps() + ap2.getPontosNps()) / 2.0);
            
            apFinal.setAtingimentoPerdidos(ap1.getAtingimentoPerdidos().add(ap2.getAtingimentoPerdidos()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setPontosPerdidos((ap1.getPontosPerdidos() + ap2.getPontosPerdidos()) / 2.0);
            
            apFinal.setPontuacaoTotal(ap1.getPontuacaoTotal().add(ap2.getPontuacaoTotal()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP));
            apFinal.setTotalChamados(ap1.getTotalChamados() + ap2.getTotalChamados());
            
            // Elegibilidade final: se perdeu elegibilidade em qualquer mês, perde na campanha?
            // Ou calculamos baseado na média? O usuário disse que "a pontuação final é a média".
            // Vamos manter a elegibilidade se a média estiver dentro da meta.
            double percSla = apFinal.getAtingimentoSla().doubleValue() * 100;
            // Para reincidencia equipe precisamos da media da equipe. O mais simples é verificar se ele foi elegivel nos dois meses.
            if (!ap1.getStatusElegibilidade()) {
                apFinal.setStatusElegibilidade(false);
                apFinal.setMotivoInelegibilidade("Inelegível no Mês 1: " + ap1.getMotivoInelegibilidade());
            } else if (!ap2.getStatusElegibilidade()) {
                apFinal.setStatusElegibilidade(false);
                apFinal.setMotivoInelegibilidade("Inelegível no Mês 2: " + ap2.getMotivoInelegibilidade());
            } else {
                apFinal.setStatusElegibilidade(true);
                apFinal.setMotivoInelegibilidade(null);
            }
            
            apFinal.setDataCalculo(LocalDateTime.now());
            apuracaoRepository.save(apFinal);
        }
    }

    private ApuracaoMensal calcularParaPeriodo(Tecnico tecnico, LocalDate dataInicio, LocalDate dataFim, LocalDate mesAnoGravacao) {

        int idTecnico = tecnico.getIdTecnico();

        boolean hasCtBase = tecnico.getCtBase() != null && !tecnico.getCtBase().isEmpty();
        String equipeField = hasCtBase ? "c.ct_base" : "c.id_tecnico";
        Object equipeValue = hasCtBase ? tecnico.getCtBase() : idTecnico;

        // --- MÉTRICAS DE EQUIPE ---

        // 1. SLA (Equipe)
        String sqlSlaEquipe = "SELECT count(c.numero_chamado) as total, " +
                "COALESCE(sum(case when c.status_sla = 'DENTRO' then 1 else 0 end), 0) as dentro " +
                "FROM tb_chamado c WHERE " + equipeField
                + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        Map<String, Object> slaEquipeResult = jdbcTemplate.queryForMap(sqlSlaEquipe, equipeValue, dataInicio, dataFim);
        long totalChamadosEquipe = ((Number) slaEquipeResult.get("total")).longValue();
        long dentroSlaEquipe = ((Number) slaEquipeResult.get("dentro")).longValue();

        BigDecimal pSlaEquipe = BigDecimal.ZERO;
        if (totalChamadosEquipe > 0) {
            pSlaEquipe = BigDecimal.valueOf(dentroSlaEquipe).divide(BigDecimal.valueOf(totalChamadosEquipe), 4,
                    RoundingMode.HALF_UP);
        }

        // 2. Reincidência (Equipe)
        String sqlReincEquipe = "SELECT count(r.id_reincidencia) as qtd FROM tb_reincidencia r " +
                "JOIN tb_chamado c ON r.chamado_rrc = c.numero_chamado " +
                "WHERE " + equipeField
                + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND r.meses_rrc <= 3 " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtdReincEquipe = ((Number) jdbcTemplate.queryForMap(sqlReincEquipe, equipeValue, dataInicio, dataFim)
                .getOrDefault("qtd", 0)).longValue();

        BigDecimal pReincEquipe = BigDecimal.ZERO;
        if (totalChamadosEquipe > 0) {
            pReincEquipe = BigDecimal.valueOf(qtdReincEquipe).divide(BigDecimal.valueOf(totalChamadosEquipe), 4,
                    RoundingMode.HALF_UP);
        }

        // 3. Chamados Perdidos (Equipe)
        String sqlPerdidosEquipe = "SELECT count(c.numero_chamado) as perdidos " +
                "FROM tb_chamado c WHERE " + equipeField
                + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%') " +
                "AND (c.pp = 1 OR c.classificacao_chamado IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO') " +
                "OR c.encdesc IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO') " +
                "OR c.ofensor IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO'))";
        long qtdPerdidosEquipe = ((Number) jdbcTemplate.queryForMap(sqlPerdidosEquipe, equipeValue, dataInicio, dataFim)
                .getOrDefault("perdidos", 0)).longValue();

        BigDecimal pPerdidosEquipe = BigDecimal.ZERO;
        if (totalChamadosEquipe > 0) {
            pPerdidosEquipe = BigDecimal.valueOf(qtdPerdidosEquipe).divide(BigDecimal.valueOf(totalChamadosEquipe), 4,
                    RoundingMode.HALF_UP);
        }

        // 4. NPS (Equipe) - Buscando da nova tabela tb_nps
        String sqlNps = "SELECT count(n.id_nps) as total, " +
                "COALESCE(sum(case when n.classificacao = 'PROMOTOR' then 1 else 0 end), 0) as promotores, " +
                "COALESCE(sum(case when n.classificacao = 'DETRATOR' then 1 else 0 end), 0) as detratores " +
                "FROM tb_nps n JOIN tb_chamado c ON n.numero_chamado = c.numero_chamado " +
                "WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ?";
        Map<String, Object> npsResult = jdbcTemplate.queryForMap(sqlNps, equipeValue, dataInicio, dataFim);
        long totalNps = ((Number) npsResult.get("total")).longValue();
        long promotoresNps = ((Number) npsResult.get("promotores")).longValue();
        long detratoresNps = ((Number) npsResult.get("detratores")).longValue();

        // --- MÉTRICAS INDIVIDUAIS ---

        // Total Chamados Individual
        String sqlTotalIndiv = "SELECT count(c.numero_chamado) as total FROM tb_chamado c WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long totalChamadosIndiv = ((Number) jdbcTemplate.queryForMap(sqlTotalIndiv, idTecnico, dataInicio, dataFim)
                .getOrDefault("total", 0)).longValue();

        // 5. Reincidência (Individual)
        String sqlReincIndiv = "SELECT count(r.id_reincidencia) as qtd FROM tb_reincidencia r " +
                "JOIN tb_chamado c ON r.chamado_rrc = c.numero_chamado " +
                "WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND r.meses_rrc <= 3 AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtdReincIndiv = ((Number) jdbcTemplate.queryForMap(sqlReincIndiv, idTecnico, dataInicio, dataFim)
                .getOrDefault("qtd", 0)).longValue();

        BigDecimal pReincIndiv = BigDecimal.ZERO;
        if (totalChamadosIndiv > 0) {
            pReincIndiv = BigDecimal.valueOf(qtdReincIndiv).divide(BigDecimal.valueOf(totalChamadosIndiv), 4,
                    RoundingMode.HALF_UP);
        }

        // 6. Peças (Individual)
        String sqlPecasIndiv = "SELECT count(p.id_consumo) as qtd FROM tb_consumo_peca p " +
                "JOIN tb_chamado c ON p.numero_chamado = c.numero_chamado " +
                "WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND p.grupo_mercadoria IN ('HDD', 'SSD', 'PLM', 'LCD') AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtdPecasIndiv = ((Number) jdbcTemplate.queryForMap(sqlPecasIndiv, idTecnico, dataInicio, dataFim)
                .getOrDefault("qtd", 0)).longValue();

        BigDecimal pPecasIndiv = BigDecimal.ZERO;
        if (totalChamadosIndiv > 0) {
            pPecasIndiv = BigDecimal.valueOf(qtdPecasIndiv).divide(BigDecimal.valueOf(totalChamadosIndiv), 4,
                    RoundingMode.HALF_UP);
        }

        // --- CÁLCULO DE PONTOS (CIAT) ---

        // 1. SLA Equipe (Gatilho) - Peso 25
        double percSlaEquipe = pSlaEquipe.doubleValue() * 100;
        int ptsSla = 0;

        if (percSlaEquipe >= 100) {
            ptsSla = 25;
        } else if (percSlaEquipe >= 90) {
            ptsSla = 20;
        }

        // 2. Reincidência Técnica Equipe (Gatilho) - Peso 10
        double percReincEquipe = pReincEquipe.doubleValue() * 100;
        int ptsReincEquipe = 0;
        if (percReincEquipe <= 7)
            ptsReincEquipe = 10;
        else if (percReincEquipe <= 10)
            ptsReincEquipe = 5;

        // 3. Chamados Perdidos Equipe - Peso 20
        double percPerdidosEquipe = pPerdidosEquipe.doubleValue() * 100;
        int ptsPerdidos = 0;
        if (percPerdidosEquipe <= 1)
            ptsPerdidos = 20;
        else if (percPerdidosEquipe <= 2)
            ptsPerdidos = 15;

        // 4. NPS Equipe - Peso 17.5
        double ptsNps = 0;
        BigDecimal pNps = BigDecimal.ZERO;
        if (totalNps == 0) {
            ptsNps = 17.5; // Sem dados = Meta Atingida (para não penalizar a ausência da tb_nps)
            pNps = BigDecimal.ONE; // Considera 100% (1.0) se não tem avaliações ruins
        } else {
            if (promotoresNps > 0 && detratoresNps == 0)
                ptsNps = 17.5;
            double score = (double) (promotoresNps - detratoresNps) / totalNps;
            pNps = BigDecimal.valueOf(Math.max(score, 0));
        }

        // 5. Reincidência Técnica Individual - Peso 15
        double percReincIndiv = pReincIndiv.doubleValue() * 100;
        int ptsReincIndivPts = 0;
        if (percReincIndiv <= 7)
            ptsReincIndivPts = 15;
        else if (percReincIndiv <= 10)
            ptsReincIndivPts = 10;

        // 6. Eficiência no uso de Peças Individual - Peso 12.5
        double percPecasIndiv = pPecasIndiv.doubleValue() * 100;
        double ptsPecasDouble = (percPecasIndiv <= 25) ? 12.5 : 0;

        // Somatório Total (Máximo 100)
        double totalPontos = ptsSla + ptsReincEquipe + ptsPerdidos + ptsNps + ptsReincIndivPts + ptsPecasDouble;

        ApuracaoMensal apuracao = apuracaoRepository
                .findFirstByTecnicoIdTecnicoAndMesAno(idTecnico, dataInicio)
                .orElse(ApuracaoMensal.builder()
                        .tecnico(tecnico)
                        .mesAno(mesAnoGravacao)
                        .build());

        apuracao.setAtingimentoSla(pSlaEquipe); // Salvando o da equipe que é o peso maior
        apuracao.setPontosSla((double) ptsSla);
        apuracao.setAtingimentoReincidencia(pReincIndiv); // Salvando individual para ver o desempenho direto dele
        apuracao.setPontosReincidencia((double) (ptsReincEquipe + ptsReincIndivPts));
        apuracao.setAtingimentoPecas(pPecasIndiv);
        apuracao.setPontosPecas(ptsPecasDouble);
        apuracao.setAtingimentoPerdidos(pPerdidosEquipe);
        apuracao.setPontosPerdidos((double) ptsPerdidos);
        apuracao.setAtingimentoNps(pNps);
        apuracao.setPontosNps(ptsNps);
        apuracao.setPontuacaoTotal(BigDecimal.valueOf(totalPontos));
        apuracao.setTotalChamados((int) totalChamadosIndiv);

        // Verificação de Gatilhos para Elegibilidade (Usando as métricas de Equipe que
        // são as classificadas como Gatilho)
        if (totalChamadosIndiv > 0) {
            if (percSlaEquipe < 90) {
                apuracao.setStatusElegibilidade(false);
                apuracao.setMotivoInelegibilidade("SLA Equipe Abaixo da Meta (<90%)");
            } else if (percReincEquipe > 10) {
                apuracao.setStatusElegibilidade(false);
                apuracao.setMotivoInelegibilidade("Reincidência Equipe Acima do Limite (>10%)");
            } else if (totalPontos < 70) {
                apuracao.setStatusElegibilidade(false);
                apuracao.setMotivoInelegibilidade("Pontuação final abaixo de 70 pontos");
            } else {
                apuracao.setStatusElegibilidade(true);
                apuracao.setMotivoInelegibilidade(null);
            }
        } else {
            apuracao.setStatusElegibilidade(false);
            apuracao.setMotivoInelegibilidade("Sem chamados no período");
        }

        apuracao.setDataCalculo(LocalDateTime.now());
        return apuracaoRepository.save(apuracao);
    }

}