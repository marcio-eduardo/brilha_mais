package br.com.positivo.brilhamais.repositories;

import br.com.positivo.brilhamais.models.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Integer> {
    Optional<Tecnico> findByMatricula(String matricula);
    Optional<Tecnico> findFirstByNomeCompletoContainingIgnoreCaseAndCtBaseIgnoreCase(String nomeCompleto, String ctBase);

    @Query(value = "SELECT t.* FROM tb_tecnico t JOIN tb_base_atp b ON t.ct_base = b.ct_codigo WHERE t.nome_completo ILIKE concat('%', :nome, '%') AND b.uf ILIKE :estado LIMIT 1", nativeQuery = true)
    Optional<Tecnico> findByNomeAndEstadoNative(@Param("nome") String nome, @Param("estado") String estado);
}
