package br.com.positivo.brilhamais.services;

import org.springframework.stereotype.Component;

@Component
public class RegrasElegibilidadeCiat {

    public VereditoElegibilidade avaliar(double pontosTotal, double percSlaEquipe, int totalChamados) {
        if (totalChamados == 0) {
            return new VereditoElegibilidade(false, "Sem chamados no período");
        }
        if (pontosTotal < 70.0) {
            return new VereditoElegibilidade(false, "Pontuação final abaixo de 70 pontos");
        }
        if (percSlaEquipe < 90.0) {
            return new VereditoElegibilidade(false, "SLA Equipe Abaixo da Meta (<90%)");
        }
        
        return new VereditoElegibilidade(true, null);
    }

    public record VereditoElegibilidade(boolean elegivel, String motivo) {}
}
