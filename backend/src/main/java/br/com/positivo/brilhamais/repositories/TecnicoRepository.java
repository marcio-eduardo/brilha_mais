package br.com.positivo.brilhamais.repositories;

import br.com.positivo.brilhamais.models.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Integer> {
    Optional<Tecnico> findByMatricula(String matricula);
}
