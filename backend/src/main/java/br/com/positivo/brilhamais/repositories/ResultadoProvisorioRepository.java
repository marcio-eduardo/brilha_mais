package br.com.positivo.brilhamais.repositories;

import br.com.positivo.brilhamais.models.ResultadoProvisorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultadoProvisorioRepository extends JpaRepository<ResultadoProvisorio, Long> {
    
    @Query("SELECT r FROM ResultadoProvisorio r ORDER BY r.resultadoFinal DESC")
    List<ResultadoProvisorio> findRankingOrderByResultadoFinalDesc();

    List<ResultadoProvisorio> findByMesCampanha(String mesCampanha);

    ResultadoProvisorio findFirstByOrderByMesCampanhaDesc();
}
