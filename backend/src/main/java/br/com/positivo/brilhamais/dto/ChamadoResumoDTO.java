package br.com.positivo.brilhamais.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChamadoResumoDTO {
    private String id;        // Mapeado de numeroChamado (ex: "OS-202605149")
    private String desc;      // Mapeado de equipamento/segmento/etc (ex: "Troca de Placa Mãe - Desktop")
    private String status;    // Mapeado de statusSla (ex: "Dentro SLA" ou "Fora SLA")
    private Boolean isLate;   // True se "Fora SLA"
    private String time;      // Mapeado de dataEncerramento formatada
    private String pecasUtilizadas; // Ex: "SSD 240GB, PLACA MÃE"
    private String textoEncerramento; // Ex: "Troca efetuada com sucesso"
}
