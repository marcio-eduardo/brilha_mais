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
    private final RegrasElegibilidadeCiat regrasCiat;

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * ?")
    public void rotinaDiariaCalculo() {
        calcularEProcessarMes(LocalDate.now().withDayOfMonth(1));
    }

    @Transactional
    public void calcularEProcessarMes(LocalDate ignoredParam) {
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        if (campanhaAtiva == null) return;

        LocalDate dataInicio = campanhaAtiva.getDataInicio();
        LocalDate dataFim = campanhaAtiva.getDataFim();

        tecnicoRepository.findAll().stream()
            .filter(Tecnico::getAtivo)
            .forEach(tecnico -> processarTecnico(tecnico, dataInicio, dataFim));
    }

    @Transactional
    public void calcularEProcessarTecnico(String matricula) {
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        if (campanhaAtiva == null) return;

        Tecnico tecnico = tecnicoRepository.findByMatricula(matricula).orElse(null);
        if (tecnico == null || !tecnico.getAtivo() || "00000".equals(matricula)) return;

        processarTecnico(tecnico, campanhaAtiva.getDataInicio(), campanhaAtiva.getDataFim());
    }

    private void processarTecnico(Tecnico tecnico, LocalDate dataInicioCampanha, LocalDate dataFimCampanha) {
        // Mês 1
        LocalDate m1Inicio = dataInicioCampanha.withDayOfMonth(1);
        LocalDate m1Fim = dataFimCampanha.isBefore(m1Inicio.withDayOfMonth(m1Inicio.lengthOfMonth())) ? dataFimCampanha : m1Inicio.withDayOfMonth(m1Inicio.lengthOfMonth());
        ApuracaoMensal ap1 = calcularParaPeriodo(tecnico, m1Inicio, m1Fim, m1Inicio);
        
        // Mês 2
        LocalDate m2Inicio = m1Fim.plusDays(1).withDayOfMonth(1);
        if (m2Inicio.isAfter(dataFimCampanha)) return;
        
        LocalDate m2Fim = dataFimCampanha.isBefore(m2Inicio.withDayOfMonth(m2Inicio.lengthOfMonth())) ? dataFimCampanha : m2Inicio.withDayOfMonth(m2Inicio.lengthOfMonth());
        ApuracaoMensal ap2 = calcularParaPeriodo(tecnico, m2Inicio, m2Fim, m2Inicio);

        // Média Geral
        ApuracaoMensal apFinal = apuracaoRepository
            .findFirstByTecnicoIdTecnicoAndMesAno(tecnico.getIdTecnico(), dataFimCampanha)
            .orElse(ApuracaoMensal.builder().tecnico(tecnico).mesAno(dataFimCampanha).build());
                    
        apFinal.setAtingimentoSla(calcularMedia(ap1.getAtingimentoSla(), ap2.getAtingimentoSla()));
        apFinal.setPontosSla((ap1.getPontosSla() + ap2.getPontosSla()) / 2.0);
        
        apFinal.setAtingimentoReincidencia(calcularMedia(ap1.getAtingimentoReincidencia(), ap2.getAtingimentoReincidencia()));
        apFinal.setPontosReincidencia((ap1.getPontosReincidencia() + ap2.getPontosReincidencia()) / 2.0);
        
        apFinal.setAtingimentoPecas(calcularMedia(ap1.getAtingimentoPecas(), ap2.getAtingimentoPecas()));
        apFinal.setPontosPecas((ap1.getPontosPecas() + ap2.getPontosPecas()) / 2.0);
        
        apFinal.setAtingimentoNps(calcularMedia(ap1.getAtingimentoNps(), ap2.getAtingimentoNps()));
        apFinal.setPontosNps((ap1.getPontosNps() + ap2.getPontosNps()) / 2.0);
        
        apFinal.setAtingimentoPerdidos(calcularMedia(ap1.getAtingimentoPerdidos(), ap2.getAtingimentoPerdidos()));
        apFinal.setPontosPerdidos((ap1.getPontosPerdidos() + ap2.getPontosPerdidos()) / 2.0);
        
        apFinal.setPontuacaoTotal(calcularMedia(ap1.getPontuacaoTotal(), ap2.getPontuacaoTotal()));
        apFinal.setTotalChamados(ap1.getTotalChamados() + ap2.getTotalChamados());
        
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

    private ApuracaoMensal calcularParaPeriodo(Tecnico tecnico, LocalDate dataInicio, LocalDate dataFim, LocalDate mesAnoGravacao) {
        int idTecnico = tecnico.getIdTecnico();
        boolean hasCtBase = tecnico.getCtBase() != null && !tecnico.getCtBase().isEmpty();
        String equipeField = hasCtBase ? "c.ct_base" : "c.id_tecnico";
        Object equipeValue = hasCtBase ? tecnico.getCtBase() : idTecnico;

        // Buscando Métricas Base do BD
        BigDecimal pSlaEquipe = calcularPercentualSlaEquipe(equipeField, equipeValue, dataInicio, dataFim);
        BigDecimal pReincEquipe = calcularPercentualReincidenciaEquipe(equipeField, equipeValue, dataInicio, dataFim);
        BigDecimal pPerdidosEquipe = calcularPercentualPerdidosEquipe(equipeField, equipeValue, dataInicio, dataFim);
        Map<String, Object> npsResult = buscarNps(equipeField, equipeValue, dataInicio, dataFim);
        long totalChamadosIndiv = buscarTotalChamadosIndividual(idTecnico, dataInicio, dataFim);
        BigDecimal pReincIndiv = calcularPercentualReincidenciaIndividual(idTecnico, dataInicio, dataFim);
        BigDecimal pPecasIndiv = calcularPercentualPecasIndividual(idTecnico, totalChamadosIndiv, dataInicio, dataFim);

        // Transformando Métricas em Pontos
        double percSlaEquipe = pSlaEquipe.doubleValue() * 100;
        int ptsSla = calcularPontosSla(percSlaEquipe);
        
        double percReincEquipe = pReincEquipe.doubleValue() * 100;
        int ptsReincEquipe = calcularPontosReincidenciaEquipe(percReincEquipe);
        
        double percPerdidosEquipe = pPerdidosEquipe.doubleValue() * 100;
        int ptsPerdidos = calcularPontosPerdidos(percPerdidosEquipe);
        
        double ptsNps = calcularPontosNps(npsResult);
        BigDecimal pNps = extrairPercentualNps(npsResult);
        
        double percReincIndiv = pReincIndiv.doubleValue() * 100;
        int ptsReincIndivPts = calcularPontosReincidenciaIndividual(percReincIndiv);
        
        double percPecasIndiv = pPecasIndiv.doubleValue() * 100;
        double ptsPecasDouble = (percPecasIndiv <= 25) ? 12.5 : 0;

        double totalPontos = ptsSla + ptsReincEquipe + ptsPerdidos + ptsNps + ptsReincIndivPts + ptsPecasDouble;

        // Construindo e Salvando a Entidade
        ApuracaoMensal apuracao = apuracaoRepository
                .findFirstByTecnicoIdTecnicoAndMesAno(idTecnico, dataInicio)
                .orElse(ApuracaoMensal.builder().tecnico(tecnico).mesAno(mesAnoGravacao).build());

        apuracao.setAtingimentoSla(pSlaEquipe);
        apuracao.setPontosSla((double) ptsSla);
        apuracao.setAtingimentoReincidencia(pReincIndiv);
        apuracao.setPontosReincidencia((double) ptsReincIndivPts);
        apuracao.setAtingimentoReincidenciaEquipe(pReincEquipe);
        apuracao.setPontosReincidenciaEquipe((double) ptsReincEquipe);
        apuracao.setAtingimentoPecas(pPecasIndiv);
        apuracao.setPontosPecas(ptsPecasDouble);
        apuracao.setAtingimentoPerdidos(pPerdidosEquipe);
        apuracao.setPontosPerdidos((double) ptsPerdidos);
        apuracao.setAtingimentoNps(pNps);
        apuracao.setPontosNps(ptsNps);
        apuracao.setPontuacaoTotal(BigDecimal.valueOf(totalPontos));
        apuracao.setTotalChamados((int) totalChamadosIndiv);

        // Elegibilidade Centralizada
        RegrasElegibilidadeCiat.VereditoElegibilidade veredito = regrasCiat.avaliar(totalPontos, percSlaEquipe, (int) totalChamadosIndiv);
        apuracao.setStatusElegibilidade(veredito.elegivel());
        apuracao.setMotivoInelegibilidade(veredito.motivo());

        apuracao.setDataCalculo(LocalDateTime.now());
        return apuracaoRepository.save(apuracao);
    }

    // --- MÉTODOS AUXILIARES: CÁLCULOS E REGRAS ---

    private BigDecimal calcularMedia(BigDecimal v1, BigDecimal v2) {
        return v1.add(v2).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private int calcularPontosSla(double perc) {
        if (perc >= 100) return 25;
        if (perc >= 90) return 20;
        return 0;
    }

    private int calcularPontosReincidenciaEquipe(double perc) {
        if (perc <= 7) return 10;
        if (perc <= 10) return 5;
        return 0;
    }

    private int calcularPontosPerdidos(double perc) {
        if (perc <= 1) return 20;
        if (perc <= 2) return 15;
        return 0;
    }

    private int calcularPontosReincidenciaIndividual(double perc) {
        if (perc <= 7) return 15;
        if (perc <= 10) return 10;
        return 0;
    }

    private double calcularPontosNps(Map<String, Object> result) {
        long total = ((Number) result.get("total")).longValue();
        long promotores = ((Number) result.get("promotores")).longValue();
        long detratores = ((Number) result.get("detratores")).longValue();
        
        if (total == 0 || (promotores > 0 && detratores == 0)) return 17.5;
        return 0.0;
    }

    private BigDecimal extrairPercentualNps(Map<String, Object> result) {
        long total = ((Number) result.get("total")).longValue();
        long promotores = ((Number) result.get("promotores")).longValue();
        long detratores = ((Number) result.get("detratores")).longValue();
        if (total == 0) return BigDecimal.ONE;
        double score = (double) (promotores - detratores) / total;
        return BigDecimal.valueOf(Math.max(score, 0));
    }

    // --- MÉTODOS AUXILIARES: CONSULTAS JDBC ---

    private BigDecimal calcularPercentualSlaEquipe(String equipeField, Object equipeValue, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT count(c.numero_chamado) as total, COALESCE(sum(case when c.status_sla = 'DENTRO' then 1 else 0 end), 0) as dentro " +
                "FROM tb_chamado c WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        Map<String, Object> r = jdbcTemplate.queryForMap(sql, equipeValue, inicio, fim);
        long t = ((Number) r.get("total")).longValue();
        long d = ((Number) r.get("dentro")).longValue();
        return t > 0 ? BigDecimal.valueOf(d).divide(BigDecimal.valueOf(t), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calcularPercentualReincidenciaEquipe(String equipeField, Object equipeValue, LocalDate inicio, LocalDate fim) {
        String sql1 = "SELECT count(r.id_reincidencia) as qtd FROM tb_reincidencia r JOIN tb_chamado c ON r.chamado_rrc = c.numero_chamado " +
                "WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND r.meses_rrc <= 3 " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtd = ((Number) jdbcTemplate.queryForMap(sql1, equipeValue, inicio, fim).getOrDefault("qtd", 0)).longValue();

        String sql2 = "SELECT count(c.numero_chamado) as total FROM tb_chamado c WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%') AND (c.encdesc IS NULL OR c.encdesc NOT ILIKE '%FALHA NÃO ENCONTRADA%')";
        long base = ((Number) jdbcTemplate.queryForMap(sql2, equipeValue, inicio, fim).getOrDefault("total", 0)).longValue();
        
        return base > 0 ? BigDecimal.valueOf(qtd).divide(BigDecimal.valueOf(base), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calcularPercentualPerdidosEquipe(String equipeField, Object equipeValue, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT count(c.numero_chamado) as perdidos FROM tb_chamado c WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%') " +
                "AND (c.pp = 1 OR c.classificacao_chamado IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO') " +
                "OR c.encdesc IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO') " +
                "OR c.ofensor IN ('TRANSFERENCIA ENTRE BASES', 'PERFORMANCE FALHA GESTAO'))";
        long qtd = ((Number) jdbcTemplate.queryForMap(sql, equipeValue, inicio, fim).getOrDefault("perdidos", 0)).longValue();
        
        String sqlT = "SELECT count(c.numero_chamado) as total FROM tb_chamado c WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long t = ((Number) jdbcTemplate.queryForMap(sqlT, equipeValue, inicio, fim).getOrDefault("total", 0)).longValue();

        return t > 0 ? BigDecimal.valueOf(qtd).divide(BigDecimal.valueOf(t), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private Map<String, Object> buscarNps(String equipeField, Object equipeValue, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT count(n.id_nps) as total, COALESCE(sum(case when n.classificacao = 'PROMOTOR' then 1 else 0 end), 0) as promotores, " +
                "COALESCE(sum(case when n.classificacao = 'DETRATOR' then 1 else 0 end), 0) as detratores " +
                "FROM tb_nps n JOIN tb_chamado c ON n.numero_chamado = c.numero_chamado " +
                "WHERE " + equipeField + " = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ?";
        return jdbcTemplate.queryForMap(sql, equipeValue, inicio, fim);
    }

    private long buscarTotalChamadosIndividual(int idTecnico, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT count(c.numero_chamado) as total FROM tb_chamado c WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        return ((Number) jdbcTemplate.queryForMap(sql, idTecnico, inicio, fim).getOrDefault("total", 0)).longValue();
    }

    private BigDecimal calcularPercentualReincidenciaIndividual(int idTecnico, LocalDate inicio, LocalDate fim) {
        String sql1 = "SELECT count(r.id_reincidencia) as qtd FROM tb_reincidencia r JOIN tb_tecnico t ON UPPER(TRIM(r.tecnico_anterior)) = UPPER(TRIM(t.nome_completo)) " +
                "JOIN tb_chamado c ON r.chamado_rrc = c.numero_chamado " +
                "WHERE t.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? AND r.meses_rrc <= 3 AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtd = ((Number) jdbcTemplate.queryForMap(sql1, idTecnico, inicio, fim).getOrDefault("qtd", 0)).longValue();

        String sql2 = "SELECT count(c.numero_chamado) as total FROM tb_chamado c WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%') AND (c.encdesc IS NULL OR c.encdesc NOT ILIKE '%FALHA NÃO ENCONTRADA%')";
        long base = ((Number) jdbcTemplate.queryForMap(sql2, idTecnico, inicio, fim).getOrDefault("total", 0)).longValue();

        return base > 0 ? BigDecimal.valueOf(qtd).divide(BigDecimal.valueOf(base), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    private BigDecimal calcularPercentualPecasIndividual(int idTecnico, long totalChamados, LocalDate inicio, LocalDate fim) {
        String sql = "SELECT count(p.id_consumo) as qtd FROM tb_consumo_peca p JOIN tb_chamado c ON p.numero_chamado = c.numero_chamado " +
                "WHERE c.id_tecnico = ? AND c.data_encerramento >= ? AND c.data_encerramento <= ? " +
                "AND p.grupo_mercadoria IN ('11', '11.0', '73', '73.0', '83', '83.0', '89', '89.0', '101', '101.0', '34', '34.0', '1102', '1102.0', '4007', '4007.0') " +
                "AND (c.status_chamado IS NULL OR c.status_chamado NOT ILIKE '%CANCELADO%')";
        long qtd = ((Number) jdbcTemplate.queryForMap(sql, idTecnico, inicio, fim).getOrDefault("qtd", 0)).longValue();

        return totalChamados > 0 ? BigDecimal.valueOf(qtd).divide(BigDecimal.valueOf(totalChamados), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

}