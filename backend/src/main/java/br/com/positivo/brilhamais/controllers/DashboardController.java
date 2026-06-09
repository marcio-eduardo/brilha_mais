package br.com.positivo.brilhamais.controllers;

import br.com.positivo.brilhamais.dto.RankingDTO;
import br.com.positivo.brilhamais.services.DashboardService;
import br.com.positivo.brilhamais.services.MotorCalculoService;
import br.com.positivo.brilhamais.repositories.CampanhaRepository;
import br.com.positivo.brilhamais.models.Campanha;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final MotorCalculoService motorCalculoService;
    private final CampanhaRepository campanhaRepository;

    @GetMapping("/ranking")
    public ResponseEntity<List<RankingDTO>> getRanking(
            @RequestParam(name = "mesAno", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mesAno
    ) {
        if (mesAno == null) {
            Campanha campanha = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc().orElse(null);
            if (campanha != null) {
                mesAno = campanha.getDataFim(); // Média final (ou mês final)
            } else {
                mesAno = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            }
        }
        return ResponseEntity.ok(dashboardService.getRankingMensal(mesAno));
    }

    @PostMapping("/calcular")
    public ResponseEntity<String> forceCalculate(
            @RequestParam(name = "mesAno", required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mesAno
    ) {
        motorCalculoService.calcularEProcessarMes(mesAno);
        return ResponseEntity.ok("Cálculo e consolidação em lote concluídos com sucesso.");
    }
    
    @PostMapping("/calcular-tecnico")
    public ResponseEntity<String> forceCalculateTecnico(
            @RequestParam(name = "matricula") String matricula
    ) {
        motorCalculoService.calcularEProcessarTecnico(matricula);
        return ResponseEntity.ok("Cálculo individual concluído para o técnico com matrícula " + matricula);
    }
}
