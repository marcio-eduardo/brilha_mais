package br.com.positivo.brilhamais.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerificarTecnicoRequest {
    @NotBlank
    private String nome;

    @NotBlank
    private String estado;
}
