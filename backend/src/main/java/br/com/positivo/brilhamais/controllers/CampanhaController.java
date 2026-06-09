package br.com.positivo.brilhamais.controllers;

import br.com.positivo.brilhamais.models.Campanha;
import br.com.positivo.brilhamais.repositories.CampanhaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/campanha")
@RequiredArgsConstructor
public class CampanhaController {

    private final CampanhaRepository campanhaRepository;

    @GetMapping("/ativa")
    public ResponseEntity<Campanha> getCampanhaAtiva() {
        return campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/ativa")
    public ResponseEntity<Campanha> saveCampanhaAtiva(@RequestBody Campanha request) {
        // Find existing active campaign or create new
        Campanha campanha = campanhaRepository.findFirstByAtivaTrueOrderByIdCampanhaDesc()
                .orElse(Campanha.builder().ativa(true).build());
        
        campanha.setDataInicio(request.getDataInicio());
        campanha.setDataFim(request.getDataFim());
        
        return ResponseEntity.ok(campanhaRepository.save(campanha));
    }
}
