package br.com.positivo.brilhamais.repositories;

import br.com.positivo.brilhamais.models.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampanhaRepository extends JpaRepository<Campanha, Integer> {
    
    Optional<Campanha> findFirstByAtivaTrueOrderByIdCampanhaDesc();
    
}
