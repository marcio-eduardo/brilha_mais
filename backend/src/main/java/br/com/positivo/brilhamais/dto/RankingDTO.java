package br.com.positivo.brilhamais.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RankingDTO {
    private Integer posicaoRanking;
    private String tecnico;
    
    private Double pontosTotal;
    
    private BigDecimal percentualPerdidos;
    private Double pontosPerdidos;
    
    private BigDecimal percentualSla;
    private Double pontosSla;
    
    private BigDecimal percentualReincidencia;
    private Double pontosReincidencia;
    private BigDecimal percentualReincidenciaEquipe;
    private Double pontosReincidenciaEquipe;
    private Integer quantidadeProdutividade;
    private Double pontosProdutividade;
    
    private BigDecimal percentualEficienciaPecas;
    private Double pontosPecas;
    
    private BigDecimal npsScore;
    private Double pontosNps;
    private Integer npsPromotores;
    private Integer npsDetratores;
    
    private java.util.List<ChamadoResumoDTO> ultimosChamados;
    
    private Boolean elegivel;
    private String motivoInelegibilidade;
    private LocalDate mesReferencia;
    private String matricula;
    
    private java.util.List<HistoricoDTO> historico;
}
