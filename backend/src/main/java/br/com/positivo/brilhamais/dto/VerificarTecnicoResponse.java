package br.com.positivo.brilhamais.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificarTecnicoResponse {
    private Integer id;
    private String nomeCompleto;
    private String ctBase;
}
