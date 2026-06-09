package br.com.positivo.brilhamais.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_chamado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chamado {

    @Id
    @Column(name = "numero_chamado")
    private Long numeroChamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tecnico")
    private Tecnico tecnico;

    @Column(name = "ct_base")
    private String ctBase;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @Column(name = "segmento")
    private String segmento;

    @Column(name = "equipamento")
    private String equipamento;

    @Column(name = "projeto")
    private String projeto;

    @Column(name = "status_sla")
    private String statusSla;

    @Column(name = "tempo_atendimento_min")
    private Integer tempoAtendimentoMin;

    @Column(name = "classificacao_chamado")
    private String classificacaoChamado;

    @Column(name = "encdesc")
    private String encdesc;
}
