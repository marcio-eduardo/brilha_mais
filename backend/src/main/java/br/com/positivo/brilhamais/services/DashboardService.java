package br.com.positivo.brilhamais.services;

import br.com.positivo.brilhamais.dto.ChamadoResumoDTO;
import br.com.positivo.brilhamais.dto.HistoricoDTO;
import br.com.positivo.brilhamais.dto.RankingDTO;
import br.com.positivo.brilhamais.models.ApuracaoMensal;
import br.com.positivo.brilhamais.models.Campanha;
import br.com.positivo.brilhamais.models.Chamado;
import br.com.positivo.brilhamais.models.ResultadoProvisorio;
import br.com.positivo.brilhamais.repositories.ApuracaoMensalRepository;
import br.com.positivo.brilhamais.repositories.CampanhaRepository;
import br.com.positivo.brilhamais.repositories.ChamadoRepository;
import br.com.positivo.brilhamais.repositories.ResultadoProvisorioRepository;
import br.com.positivo.brilhamais.repositories.TecnicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApuracaoMensalRepository apuracaoRepository;
    private final ChamadoRepository chamadoRepository;
    private final TecnicoRepository tecnicoRepository;
    private final CampanhaRepository campanhaRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ResultadoProvisorioRepository resultadoProvisorioRepository;
    private final RegrasElegibilidadeCiat regrasCiat;

    private static final DateTimeFormatter FORMATTER_MES = DateTimeFormatter.ofPattern("MMM");
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("dd/MM, HH:mm");

    private static final String[] MESES = {"", "Janeiro", "Fevereiro", "Marco", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    public List<RankingDTO> getRankingMensal(LocalDate mesAno) {
        String mesCampanha = String.format("%02d-%s", mesAno.getMonthValue(), MESES[mesAno.getMonthValue()]);
        List<ResultadoProvisorio> provisorios = resultadoProvisorioRepository.findByMesCampanha(mesCampanha);
        
        // Lógica de Fallback: Se não encontrar dados para o mês exato, pega a última planilha ingerida
        if (provisorios.isEmpty()) {
            ResultadoProvisorio latest = resultadoProvisorioRepository.findFirstByOrderByMesCampanhaDesc();
            if (latest != null) {
                provisorios = resultadoProvisorioRepository.findByMesCampanha(latest.getMesCampanha());
            }
        }
        
        if (!provisorios.isEmpty()) {
            return buildRankingFromProvisorio(provisorios, mesAno);
        }

        return buildRankingFromApuracao(mesAno);
    }

    // --- Fluxo 1: Resultado Provisório (Planilha) ---

    private List<RankingDTO> buildRankingFromProvisorio(List<ResultadoProvisorio> provisorios, LocalDate mesAno) {
        Map<String, Integer> nomeParaId = new HashMap<>();
        Map<String, String> nomeParaMatricula = new HashMap<>();
        tecnicoRepository.findAll().forEach(t -> {
            if (t.getNomeCompleto() != null) {
                nomeParaId.put(t.getNomeCompleto().trim().toUpperCase(), t.getIdTecnico());
                nomeParaMatricula.put(t.getNomeCompleto().trim().toUpperCase(), t.getMatricula());
            }
        });

        List<Integer> tecnicoIds = provisorios.stream()
            .filter(p -> p.getOperacaoTecnico() != null)
            .map(p -> nomeParaId.get(p.getOperacaoTecnico().trim().toUpperCase()))
            .filter(id -> id != null)
            .collect(Collectors.toList());

        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        LocalDateTime dataInicio = campanhaAtiva != null ? campanhaAtiva.getDataInicio().atStartOfDay() : mesAno.withDayOfMonth(1).atStartOfDay();
        LocalDateTime dataFim = campanhaAtiva != null ? campanhaAtiva.getDataFim().atTime(23, 59, 59, 999999999) : mesAno.withDayOfMonth(mesAno.lengthOfMonth()).atTime(23, 59, 59, 999999999);

        Map<Integer, List<ApuracaoMensal>> historicoPorTecnico = fetchHistoricoPorTecnico(tecnicoIds);
        Map<Integer, List<Chamado>> chamadosPorTecnico = fetchChamadosPorTecnico(tecnicoIds, dataInicio, dataFim);
        
        List<Long> chamadosIds = chamadosPorTecnico.values().stream().flatMap(List::stream).map(Chamado::getNumeroChamado).collect(Collectors.toList());
        Map<Long, Map<String, String>> detalhesChamado = fetchPecasETextosChamados(chamadosIds);

        List<RankingDTO> ranking = new ArrayList<>();
        int pos = 1;
        for (ResultadoProvisorio p : provisorios) {
            String opKey = p.getOperacaoTecnico() != null ? p.getOperacaoTecnico().trim().toUpperCase() : "";
            Integer idTecnico = nomeParaId.get(opKey);
            String matricula = nomeParaMatricula.get(opKey);

            List<ApuracaoMensal> historicoApuracao = idTecnico != null ? historicoPorTecnico.getOrDefault(idTecnico, new ArrayList<>()) : new ArrayList<>();
            
            // Filtra o histórico para não exibir meses futuros/abertos além da planilha ingerida
            int numMesProv = 12;
            if (!provisorios.isEmpty() && provisorios.get(0).getMesCampanha() != null) {
                try {
                    numMesProv = Integer.parseInt(provisorios.get(0).getMesCampanha().split("-")[0]);
                } catch (Exception e) {}
            }
            final int maxMes = numMesProv;

            List<HistoricoDTO> historico = historicoApuracao.stream()
                .map(h -> {
                    if (h.getMesAno().getDayOfMonth() == 1 && h.getMesAno().getMonthValue() > maxMes) {
                        // Mês futuro sem planilha ingerida: exibir zerado
                        String lbl = h.getMesAno().format(FORMATTER_MES);
                        return HistoricoDTO.builder()
                            .mes(lbl)
                            .percentualSla(0.0)
                            .pontosSla(0.0)
                            .percentualReincidencia(0.0)
                            .pontosReincidencia(0.0)
                            .percentualReincidenciaEquipe(0.0)
                            .pontosReincidenciaEquipe(0.0)
                            .npsScore(0.0)
                            .pontosNps(0.0)
                            .percentualEficienciaPecas(0.0)
                            .pontosPecas(0.0)
                            .percentualPerdidos(0.0)
                            .pontosPerdidos(0.0)
                            .pontosTotal(0)
                            .elegivel(true)
                            .motivoInelegibilidade("Aguardando fechamento do mês")
                            .build();
                    }
                    return mapToHistoricoDTO(h, p);
                })
                .collect(Collectors.toList());

            List<Chamado> chamadosRecentes = idTecnico != null ? chamadosPorTecnico.getOrDefault(idTecnico, new ArrayList<>()) : new ArrayList<>();
            List<ChamadoResumoDTO> ultimosChamados = chamadosRecentes.stream().limit(3)
                .map(c -> mapToChamadoResumoDTO(c, detalhesChamado.get(c.getNumeroChamado())))
                .collect(Collectors.toList());

            ranking.add(mapToRankingDTOFromProvisorio(p, pos++, matricula, mesAno, historico, ultimosChamados));
        }
        return ranking;
    }

    // --- Fluxo 2: Apuração Mensal (BD) ---

    private List<RankingDTO> buildRankingFromApuracao(LocalDate mesAno) {
        List<ApuracaoMensal> apuracoes = apuracaoRepository.findRankingByMesAno(mesAno);
        if (apuracoes.isEmpty()) return new ArrayList<>();

        List<Integer> tecnicoIds = apuracoes.stream().map(a -> a.getTecnico().getIdTecnico()).collect(Collectors.toList());
        
        Map<Integer, List<ApuracaoMensal>> historicoPorTecnico = fetchHistoricoPorTecnico(tecnicoIds);
        
        Campanha campanhaAtiva = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
        LocalDateTime dataInicio = campanhaAtiva != null ? campanhaAtiva.getDataInicio().atStartOfDay() : mesAno.withDayOfMonth(1).atStartOfDay();
        LocalDateTime dataFim = campanhaAtiva != null ? campanhaAtiva.getDataFim().atTime(23, 59, 59, 999999999) : mesAno.withDayOfMonth(mesAno.lengthOfMonth()).atTime(23, 59, 59, 999999999);
        
        Map<Integer, List<Chamado>> chamadosPorTecnico = fetchChamadosPorTecnico(tecnicoIds, dataInicio, dataFim);
        
        List<Long> chamadosIds = chamadosPorTecnico.values().stream().flatMap(List::stream).map(Chamado::getNumeroChamado).collect(Collectors.toList());
        Map<Long, Map<String, String>> detalhesChamado = fetchPecasETextosChamados(chamadosIds);

        List<RankingDTO> ranking = new ArrayList<>();
        int posicao = 1;
        
        for (ApuracaoMensal apuracao : apuracoes) {
            int idTecnico = apuracao.getTecnico().getIdTecnico();
            
            List<ApuracaoMensal> historicoApuracao = historicoPorTecnico.getOrDefault(idTecnico, new ArrayList<>());
            List<HistoricoDTO> historico = historicoApuracao.stream()
                .filter(h -> h.getMesAno().getDayOfMonth() != 1 || (h.getTotalChamados() != null && h.getTotalChamados() > 0))
                .map(h -> mapToHistoricoDTO(h, null))
                .collect(Collectors.toList());
            
            List<Chamado> chamadosRecentes = chamadosPorTecnico.getOrDefault(idTecnico, new ArrayList<>());
            List<ChamadoResumoDTO> ultimosChamados = chamadosRecentes.stream().limit(3)
                .map(c -> mapToChamadoResumoDTO(c, detalhesChamado.get(c.getNumeroChamado())))
                .collect(Collectors.toList());

            ranking.add(mapToRankingDTOFromApuracao(apuracao, posicao++, historico, ultimosChamados));
        }
        
        return ranking;
    }

    // --- Consultas Agrupadas ---

    private Map<Integer, List<ApuracaoMensal>> fetchHistoricoPorTecnico(List<Integer> tecnicoIds) {
        if (tecnicoIds.isEmpty()) return new HashMap<>();
        return apuracaoRepository.findHistoricoByTecnicoIds(tecnicoIds).stream()
            .collect(Collectors.groupingBy(h -> h.getTecnico().getIdTecnico()));
    }

    private Map<Integer, List<Chamado>> fetchChamadosPorTecnico(List<Integer> tecnicoIds, LocalDateTime dataInicio, LocalDateTime dataFim) {
        if (tecnicoIds.isEmpty()) return new HashMap<>();
        return chamadoRepository.findChamadosRecentesPorTecnicos(tecnicoIds, dataInicio, dataFim).stream()
            .collect(Collectors.groupingBy(c -> c.getTecnico().getIdTecnico()));
    }

    private Map<Long, Map<String, String>> fetchPecasETextosChamados(List<Long> chamadosIds) {
        Map<Long, Map<String, String>> result = new HashMap<>();
        if (chamadosIds.isEmpty()) return result;

        String inSql = String.join(",", Collections.nCopies(chamadosIds.size(), "?"));
        String query = "SELECT numero_chamado, descricao_peca, texto_encerrado FROM tb_consumo_peca WHERE numero_chamado IN (" + inSql + ")";
        
        jdbcTemplate.query(query, chamadosIds.toArray(), (rs) -> {
            Long num = rs.getLong("numero_chamado");
            String peca = rs.getString("descricao_peca");
            String texto = rs.getString("texto_encerrado");
            
            result.putIfAbsent(num, new HashMap<>());
            Map<String, String> data = result.get(num);
            
            if (peca != null) {
                data.merge("pecas", peca, (old, val) -> old + ", " + val);
            }
            if (texto != null && !texto.isEmpty()) {
                data.put("texto", texto);
            }
        });
        return result;
    }

    // --- Mapeadores ---

    private HistoricoDTO mapToHistoricoDTO(ApuracaoMensal h, ResultadoProvisorio p) {
        String label = h.getMesAno().getDayOfMonth() == 1 ? h.getMesAno().format(FORMATTER_MES) : "Média Final";
        boolean semChamados = h.getTotalChamados() != null && h.getTotalChamados() == 0;
        
        if (p != null && h.getMesAno().getMonthValue() == 5 && p.getMesCampanha() != null && p.getMesCampanha().contains("Maio")) {
            RegrasElegibilidadeCiat.VereditoElegibilidade veredito = regrasCiat.avaliar(
                valToDouble(p.getResultadoFinal()),
                valToDouble(p.getSlaEquipe()),
                -1 // Usado da planilha
            );

            // A planilha tem a coluna base, mas a regra do CIAT pode sobrescrever
            boolean baseElegivel = p.getElegibilidade() != null && 
                (p.getElegibilidade().trim().equalsIgnoreCase("Sim") || 
                 p.getElegibilidade().trim().equalsIgnoreCase("S") || 
                 p.getElegibilidade().trim().toLowerCase().contains("eleg") || 
                 p.getElegibilidade().trim().equals("1"));
            
            boolean finalElegivel = baseElegivel && veredito.elegivel();
            String motivoStr = veredito.elegivel() ? h.getMotivoInelegibilidade() : veredito.motivo();
            if (!baseElegivel && veredito.elegivel()) {
                motivoStr = "Critérios de elegibilidade da campanha não atingidos";
            }

            return HistoricoDTO.builder()
                .mes(label)
                .percentualSla(valToPct(p.getSlaEquipe()))
                .pontosSla(valToDouble(p.getPontosSlaEquipe()))
                .percentualReincidencia(valToPct(p.getReincidenciaIndividual()))
                .pontosReincidencia(valToDouble(p.getPontosRccIndividual()))
                .percentualReincidenciaEquipe(valToPct(p.getReincidenciaEquipe()))
                .pontosReincidenciaEquipe(valToDouble(p.getPontosRccEquipe()))
                .npsScore(valToPct(p.getNpsEquipe()))
                .pontosNps(valToDouble(p.getPontosNpsEquipe()))
                .percentualEficienciaPecas(valToPct(p.getConsumoPecasIndividual()))
                .pontosPecas(valToDouble(p.getPontosPecasIndividual()))
                .percentualPerdidos(valToPct(p.getPerdasSlaTransf()))
                .pontosPerdidos(valToDouble(p.getPontosSlaTransf()))
                .pontosTotal(p.getResultadoFinal() != null ? p.getResultadoFinal().intValue() : 0)
                .elegivel(finalElegivel)
                .motivoInelegibilidade(motivoStr)
                .build();
        }

        return HistoricoDTO.builder()
            .mes(label)
            .percentualSla(valToPct(h.getAtingimentoSla()))
            .pontosSla(semChamados ? 0.0 : valToDouble(h.getPontosSla()))
            .percentualReincidencia(valToPct(h.getAtingimentoReincidencia()))
            .pontosReincidencia(semChamados ? 0.0 : valToDouble(h.getPontosReincidencia()))
            .percentualReincidenciaEquipe(valToPct(h.getAtingimentoReincidenciaEquipe()))
            .pontosReincidenciaEquipe(semChamados ? 0.0 : valToDouble(h.getPontosReincidenciaEquipe()))
            .npsScore(valToPct(h.getAtingimentoNps()))
            .pontosNps(semChamados ? 0.0 : valToDouble(h.getPontosNps()))
            .percentualEficienciaPecas(valToPct(h.getAtingimentoPecas()))
            .pontosPecas(semChamados ? 0.0 : valToDouble(h.getPontosPecas()))
            .percentualPerdidos(valToPct(h.getAtingimentoPerdidos()))
            .pontosPerdidos(semChamados ? 0.0 : valToDouble(h.getPontosPerdidos()))
            .pontosTotal(semChamados ? 0 : valToInt(h.getPontuacaoTotal()))
            .elegivel(h.getStatusElegibilidade())
            .motivoInelegibilidade(h.getMotivoInelegibilidade())
            .build();
    }

    private ChamadoResumoDTO mapToChamadoResumoDTO(Chamado c, Map<String, String> dt) {
        String pecas = (dt != null && dt.containsKey("pecas")) ? dt.get("pecas") : "Nenhuma peça consumida";
        String textoEnc = (dt != null && dt.containsKey("texto")) ? dt.get("texto") : 
            (c.getEncdesc() != null && !c.getEncdesc().isEmpty() ? c.getEncdesc() : 
            (c.getClassificacaoChamado() != null ? c.getClassificacaoChamado() : "Sem texto de encerramento"));

        boolean isDentro = "DENTRO".equalsIgnoreCase(c.getStatusSla());
        
        return ChamadoResumoDTO.builder()
            .id("Chamado-" + c.getNumeroChamado())
            .desc(c.getEquipamento() != null ? c.getEquipamento() : (c.getSegmento() != null ? c.getSegmento() : "Chamado"))
            .status(isDentro ? "Encerrado dentro SLA" : "Encerrado fora do SLA")
            .isLate("FORA".equalsIgnoreCase(c.getStatusSla()) || "Fora SLA".equalsIgnoreCase(c.getStatusSla()))
            .time(c.getDataEncerramento() != null ? c.getDataEncerramento().format(FORMATTER_HORA) : "")
            .pecasUtilizadas(pecas)
            .textoEncerramento(textoEnc)
            .build();
    }

    private RankingDTO mapToRankingDTOFromProvisorio(ResultadoProvisorio p, int pos, String matricula, LocalDate mesAno, List<HistoricoDTO> historico, List<ChamadoResumoDTO> ultimosChamados) {
        RegrasElegibilidadeCiat.VereditoElegibilidade veredito = regrasCiat.avaliar(
            valToDouble(p.getResultadoFinal()),
            valToDouble(p.getSlaEquipe()),
            -1
        );

        boolean baseElegivel = p.getElegibilidade() != null && 
            (p.getElegibilidade().trim().equalsIgnoreCase("Sim") || 
             p.getElegibilidade().trim().equalsIgnoreCase("S") || 
             p.getElegibilidade().trim().toLowerCase().contains("eleg") || 
             p.getElegibilidade().trim().equals("1"));
             
        boolean finalElegivel = baseElegivel && veredito.elegivel();

        String motivoStr = veredito.elegivel() ? null : veredito.motivo();
        if (!baseElegivel && veredito.elegivel()) {
            motivoStr = "Critérios de elegibilidade da campanha não atingidos";
        }

        return RankingDTO.builder()
            .posicaoRanking(pos)
            .tecnico(p.getOperacaoTecnico())
            .matricula(matricula)
            .pontosTotal(valToDouble(p.getResultadoFinal()))
            .percentualSla(valToPctBD(p.getSlaEquipe()))
            .pontosSla(valToDouble(p.getPontosSlaEquipe()))
            .percentualReincidenciaEquipe(valToPctBD(p.getReincidenciaEquipe()))
            .pontosReincidenciaEquipe(valToDouble(p.getPontosRccEquipe()))
            .percentualReincidencia(valToPctBD(p.getReincidenciaIndividual()))
            .pontosReincidencia(valToDouble(p.getPontosRccIndividual()))
            .percentualEficienciaPecas(valToPctBD(p.getConsumoPecasIndividual()))
            .pontosPecas(valToDouble(p.getPontosPecasIndividual()))
            .npsScore(valToPctBD(p.getNpsEquipe()))
            .pontosNps(valToDouble(p.getPontosNpsEquipe()))
            .percentualPerdidos(valToPctBD(p.getPerdasSlaTransf()))
            .pontosPerdidos(valToDouble(p.getPontosSlaTransf()))
            .elegivel(finalElegivel)
            .motivoInelegibilidade(motivoStr)
            .mesReferencia(mesAno)
            .historico(historico)
            .ultimosChamados(ultimosChamados)
            .build();
    }

    private RankingDTO mapToRankingDTOFromApuracao(ApuracaoMensal apuracao, int pos, List<HistoricoDTO> historico, List<ChamadoResumoDTO> ultimosChamados) {
        return RankingDTO.builder()
            .posicaoRanking(pos)
            .tecnico(apuracao.getTecnico().getNomeCompleto())
            .pontosTotal(valToDouble(apuracao.getPontuacaoTotal()))
            .percentualPerdidos(valToPctBD(apuracao.getAtingimentoPerdidos()))
            .pontosPerdidos(valToDouble(apuracao.getPontosPerdidos()))
            .percentualSla(valToPctBD(apuracao.getAtingimentoSla()))
            .pontosSla(valToDouble(apuracao.getPontosSla()))
            .percentualReincidencia(valToPctBD(apuracao.getAtingimentoReincidencia()))
            .pontosReincidencia(valToDouble(apuracao.getPontosReincidencia()))
            .percentualReincidenciaEquipe(valToPctBD(apuracao.getAtingimentoReincidenciaEquipe()))
            .pontosReincidenciaEquipe(valToDouble(apuracao.getPontosReincidenciaEquipe()))
            .quantidadeProdutividade(apuracao.getTotalChamados() != null ? apuracao.getTotalChamados() : 0)
            .pontosProdutividade(valToDouble(apuracao.getPontosPecas()))
            .percentualEficienciaPecas(valToPctBD(apuracao.getAtingimentoPecas()))
            .pontosPecas(valToDouble(apuracao.getPontosPecas()))
            .npsScore(valToPctBD(apuracao.getAtingimentoNps()))
            .pontosNps(valToDouble(apuracao.getPontosNps()))
            .npsPromotores(0)
            .npsDetratores(0)
            .ultimosChamados(ultimosChamados)
            .elegivel(apuracao.getStatusElegibilidade())
            .motivoInelegibilidade(apuracao.getMotivoInelegibilidade())
            .mesReferencia(apuracao.getMesAno())
            .matricula(apuracao.getTecnico().getMatricula())
            .historico(historico)
            .build();
    }

    // --- Helpers de Valores Seguros ---

    private double valToPct(BigDecimal b) {
        return b != null ? b.multiply(new BigDecimal("100")).doubleValue() : 0.0;
    }

    private BigDecimal valToPctBD(BigDecimal b) {
        return b != null ? b.multiply(new BigDecimal("100")) : BigDecimal.ZERO;
    }

    private double valToDouble(Number n) {
        return n != null ? n.doubleValue() : 0.0;
    }

    private int valToInt(Number n) {
        return n != null ? n.intValue() : 0;
    }
}
