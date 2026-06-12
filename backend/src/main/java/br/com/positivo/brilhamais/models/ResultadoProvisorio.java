package br.com.positivo.brilhamais.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_resultado_provisorio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoProvisorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long idResultado;

    @Column(name = "operacao_tecnico")
    private String operacaoTecnico;

    @Column(name = "mes_campanha")
    private String mesCampanha;

    @Column(name = "sla_equipe")
    private BigDecimal slaEquipe;

    @Column(name = "pontos_sla_equipe")
    private BigDecimal pontosSlaEquipe;

    @Column(name = "perdas_sla_transf")
    private BigDecimal perdasSlaTransf;

    @Column(name = "pontos_sla_transf")
    private BigDecimal pontosSlaTransf;

    @Column(name = "nps_equipe")
    private BigDecimal npsEquipe;

    @Column(name = "pontos_nps_equipe")
    private BigDecimal pontosNpsEquipe;

    @Column(name = "reincidencia_equipe")
    private BigDecimal reincidenciaEquipe;

    @Column(name = "pontos_rcc_equipe")
    private BigDecimal pontosRccEquipe;

    @Column(name = "reincidencia_individual")
    private BigDecimal reincidenciaIndividual;

    @Column(name = "pontos_rcc_individual")
    private BigDecimal pontosRccIndividual;

    @Column(name = "consumo_pecas_individual")
    private BigDecimal consumoPecasIndividual;

    @Column(name = "pontos_pecas_individual")
    private BigDecimal pontosPecasIndividual;

    @Column(name = "resultado_final")
    private BigDecimal resultadoFinal;

    @Column(name = "elegibilidade")
    private String elegibilidade;
}
