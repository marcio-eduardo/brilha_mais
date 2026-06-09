package br.com.positivo.brilhamais.repositories;

import br.com.positivo.brilhamais.models.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    
    // Busca todos os chamados finalizados do técnico, utilizando a matrícula
    List<Chamado> findAllByTecnicoMatriculaAndDataEncerramentoIsNotNullOrderByDataEncerramentoDesc(String matricula);

    @Query("SELECT c FROM Chamado c WHERE c.tecnico.idTecnico IN :ids AND c.dataEncerramento IS NOT NULL AND c.dataEncerramento >= :dataInicio AND c.dataEncerramento <= :dataFim ORDER BY c.dataEncerramento DESC")
    List<Chamado> findChamadosRecentesPorTecnicos(List<Integer> ids, LocalDateTime dataInicio, LocalDateTime dataFim);
}
