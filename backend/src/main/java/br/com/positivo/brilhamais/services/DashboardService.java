package br.com.positivo.brilhamais.services;

import br.com.positivo.brilhamais.dto.RankingDTO;
import br.com.positivo.brilhamais.dto.HistoricoDTO;
import br.com.positivo.brilhamais.dto.ChamadoResumoDTO;
import br.com.positivo.brilhamais.models.ApuracaoMensal;
import br.com.positivo.brilhamais.models.Chamado;
import br.com.positivo.brilhamais.repositories.ApuracaoMensalRepository;
import br.com.positivo.brilhamais.repositories.ChamadoRepository;
import br.com.positivo.brilhamais.repositories.CampanhaRepository;
import br.com.positivo.brilhamais.models.Campanha;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApuracaoMensalRepository apuracaoRepository;
    private final ChamadoRepository chamadoRepository;
    private final CampanhaRepository campanhaRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<RankingDTO> getRankingMensal(LocalDate mesAno) {
        List<ApuracaoMensal> apuracoes = apuracaoRepository.findRankingByMesAno(mesAno);
        
        if (apuracoes.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Extrair IDs de todos os técnicos presentes no ranking
        List<Integer> tecnicoIds = apuracoes.stream()
                .map(a -> a.getTecnico().getIdTecnico())
                .collect(Collectors.toList());
        
        // Query 2 (Lote): Busca o histórico de apurações de todos os técnicos do ranking em uma única query
        List<ApuracaoMensal> todosHistoricos = apuracaoRepository.findHistoricoByTecnicoIds(tecnicoIds);
        
        // Agrupa o histórico na memória por ID do Técnico para acesso instantâneo (O(1))
        Map<Integer, List<ApuracaoMensal>> historicoPorTecnico = todosHistoricos.stream()
                .collect(Collectors.groupingBy(h -> h.getTecnico().getIdTecnico()));
        
        // Calcular datas de limite: Usa a campanha inteira para buscar os últimos chamados globalmente
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        LocalDateTime dataInicio = campanhaAtiva != null ? campanhaAtiva.getDataInicio().atStartOfDay() : mesAno.withDayOfMonth(1).atStartOfDay();
        LocalDateTime dataFim = campanhaAtiva != null ? campanhaAtiva.getDataFim().atTime(23, 59, 59, 999999999) : mesAno.withDayOfMonth(mesAno.lengthOfMonth()).atTime(23, 59, 59, 999999999);
        
        // Query 3 (Lote): Busca em massa os chamados finalizados do mês para todos os técnicos do ranking de uma só vez
        List<Chamado> todosChamados = chamadoRepository.findChamadosRecentesPorTecnicos(tecnicoIds, dataInicio, dataFim);
        
        // Agrupa os chamados em memória por ID do Técnico (eles já vêm ordenados por dataEncerramento DESC do banco)
        Map<Integer, List<Chamado>> chamadosPorTecnico = todosChamados.stream()
                .collect(Collectors.groupingBy(c -> c.getTecnico().getIdTecnico()));
        
        // --- BUSCA OTIMIZADA DE PEÇAS (Evita N+1) ---
        List<Long> chamadosIds = todosChamados.stream().map(Chamado::getNumeroChamado).collect(Collectors.toList());
        Map<Long, String> pecasPorChamado = new java.util.HashMap<>();
        Map<Long, String> textoEncerramentoPorChamado = new java.util.HashMap<>();
        if (!chamadosIds.isEmpty()) {
            // Divide in chunks if necessary, but usually recent tickets for ranking are not huge per month.
            // Using a simple IN clause string building for JdbcTemplate (since IN with List is easier with NamedParameterJdbcTemplate, but we can do a simple join)
            String inSql = String.join(",", java.util.Collections.nCopies(chamadosIds.size(), "?"));
            String pecasQuery = "SELECT numero_chamado, descricao_peca, texto_encerrado FROM tb_consumo_peca WHERE numero_chamado IN (" + inSql + ")";
            jdbcTemplate.query(pecasQuery, chamadosIds.toArray(), (rs) -> {
                Long numeroChamado = rs.getLong("numero_chamado");
                String peca = rs.getString("descricao_peca");
                String texto = rs.getString("texto_encerrado");
                pecasPorChamado.merge(numeroChamado, peca, (old, val) -> old + ", " + val);
                if (texto != null && !texto.isEmpty()) {
                    textoEncerramentoPorChamado.put(numeroChamado, texto);
                }
            });
        }
        // ---------------------------------------------
        
        List<RankingDTO> ranking = new ArrayList<>();
        DateTimeFormatter formatterMes = DateTimeFormatter.ofPattern("MMM");
        DateTimeFormatter formatterHora = DateTimeFormatter.ofPattern("dd/MM, HH:mm");
        
        int posicao = 1;
        for (ApuracaoMensal apuracao : apuracoes) {
            int idTecnico = apuracao.getTecnico().getIdTecnico();
            
            // Busca o histórico em memória (evita Query de banco)
            List<ApuracaoMensal> historicoApuracao = historicoPorTecnico.getOrDefault(idTecnico, new ArrayList<>());
            List<HistoricoDTO> historico = historicoApuracao.stream()
                .map(h -> {
                    String label = h.getMesAno().getDayOfMonth() == 1 ? h.getMesAno().format(formatterMes) : "Média Final";
                    return HistoricoDTO.builder()
                        .mes(label)
                        .percentualSla(h.getAtingimentoSla() != null ? h.getAtingimentoSla().multiply(new BigDecimal("100")).doubleValue() : 0.0)
                        .pontosSla(h.getPontosSla() != null ? h.getPontosSla() : 0.0)
                        .percentualReincidencia(h.getAtingimentoReincidencia() != null ? h.getAtingimentoReincidencia().multiply(new BigDecimal("100")).doubleValue() : 0.0)
                        .pontosReincidencia(h.getPontosReincidencia() != null ? h.getPontosReincidencia() : 0.0)
                        .npsScore(h.getAtingimentoNps() != null ? h.getAtingimentoNps().multiply(new BigDecimal("100")).doubleValue() : 0.0)
                        .pontosNps(h.getPontosNps() != null ? h.getPontosNps() : 0.0)
                        .percentualEficienciaPecas(h.getAtingimentoPecas() != null ? h.getAtingimentoPecas().multiply(new BigDecimal("100")).doubleValue() : 0.0)
                        .pontosPecas(h.getPontosPecas() != null ? h.getPontosPecas() : 0.0)
                        .percentualPerdidos(h.getAtingimentoPerdidos() != null ? h.getAtingimentoPerdidos().multiply(new BigDecimal("100")).doubleValue() : 0.0)
                        .pontosPerdidos(h.getPontosPerdidos() != null ? h.getPontosPerdidos() : 0.0)
                        .pontosTotal(h.getPontuacaoTotal() != null ? h.getPontuacaoTotal().intValue() : 0)
                        .elegivel(h.getStatusElegibilidade())
                        .motivoInelegibilidade(h.getMotivoInelegibilidade())
                        .build();
                })
                .collect(Collectors.toList());
            
            // Busca os chamados em memória e limita aos 3 primeiros (evita Query de banco)
            List<Chamado> chamadosRecentes = chamadosPorTecnico.getOrDefault(idTecnico, new ArrayList<>());
            List<ChamadoResumoDTO> ultimosChamados = chamadosRecentes.stream()
                .limit(3)
                .map(c -> ChamadoResumoDTO.builder()
                        .id("Chamado-" + c.getNumeroChamado())
                        .desc(c.getEquipamento() != null ? c.getEquipamento() : (c.getSegmento() != null ? c.getSegmento() : "Chamado"))
                        .status("DENTRO".equalsIgnoreCase(c.getStatusSla()) ? "Encerrado dentro SLA" : "Encerrado fora do SLA")
                        .isLate("FORA".equalsIgnoreCase(c.getStatusSla()) || "Fora SLA".equalsIgnoreCase(c.getStatusSla()))
                        .time(c.getDataEncerramento() != null ? c.getDataEncerramento().format(formatterHora) : "")
                        .pecasUtilizadas(pecasPorChamado.getOrDefault(c.getNumeroChamado(), "Nenhuma peça consumida"))
                        .textoEncerramento(textoEncerramentoPorChamado.containsKey(c.getNumeroChamado()) ? textoEncerramentoPorChamado.get(c.getNumeroChamado()) : (c.getEncdesc() != null && !c.getEncdesc().isEmpty() ? c.getEncdesc() : (c.getClassificacaoChamado() != null ? c.getClassificacaoChamado() : "Sem texto de encerramento")))
                        .build())
                .collect(Collectors.toList());

            ranking.add(RankingDTO.builder()
                    .posicaoRanking(posicao++)
                    .tecnico(apuracao.getTecnico().getNomeCompleto())
                    .pontosTotal(apuracao.getPontuacaoTotal() != null ? apuracao.getPontuacaoTotal().doubleValue() : 0.0)
                    .percentualPerdidos(apuracao.getAtingimentoPerdidos() != null ? apuracao.getAtingimentoPerdidos().multiply(new BigDecimal("100")) : BigDecimal.ZERO)
                    .pontosPerdidos(apuracao.getPontosPerdidos() != null ? apuracao.getPontosPerdidos() : 0.0)
                    .percentualSla(apuracao.getAtingimentoSla() != null ? apuracao.getAtingimentoSla().multiply(new BigDecimal("100")) : BigDecimal.ZERO)
                    .pontosSla(apuracao.getPontosSla() != null ? apuracao.getPontosSla() : 0.0)
                    .percentualReincidencia(apuracao.getAtingimentoReincidencia() != null ? apuracao.getAtingimentoReincidencia().multiply(new BigDecimal("100")) : BigDecimal.ZERO)
                    .pontosReincidencia(apuracao.getPontosReincidencia() != null ? apuracao.getPontosReincidencia() : 0.0)
                    .quantidadeProdutividade(apuracao.getTotalChamados() != null ? apuracao.getTotalChamados() : 0)
                    .pontosProdutividade(apuracao.getPontosPecas() != null ? apuracao.getPontosPecas() : 0.0)
                    .percentualEficienciaPecas(apuracao.getAtingimentoPecas() != null ? apuracao.getAtingimentoPecas().multiply(new BigDecimal("100")) : BigDecimal.ZERO)
                    .pontosPecas(apuracao.getPontosPecas() != null ? apuracao.getPontosPecas() : 0.0)
                    .npsScore(apuracao.getAtingimentoNps() != null ? apuracao.getAtingimentoNps().multiply(new BigDecimal("100")) : BigDecimal.ZERO)
                    .pontosNps(apuracao.getPontosNps() != null ? apuracao.getPontosNps() : 0.0)
                    .npsPromotores(0) // Preenchimento dinâmico se a tabela de NPS for atrelada futuramente
                    .npsDetratores(0)
                    .ultimosChamados(ultimosChamados)
                    .elegivel(apuracao.getStatusElegibilidade())
                    .motivoInelegibilidade(apuracao.getMotivoInelegibilidade())
                    .mesReferencia(apuracao.getMesAno())
                    .matricula(apuracao.getTecnico().getMatricula())
                    .historico(historico)
                    .build());
        }
        
        return ranking;
    }
}
