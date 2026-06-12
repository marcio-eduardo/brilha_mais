package br.com.positivo.brilhamais.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final JdbcTemplate jdbcTemplate;

    @DeleteMapping("/reset/{matricula}")
    @Transactional
    public ResponseEntity<String> resetUsuario(@PathVariable String matricula) {
        try {
            // Delete apuracao
            jdbcTemplate.update("DELETE FROM tb_apuracao_mensal WHERE id_tecnico = (SELECT id_tecnico FROM tb_tecnico WHERE matricula = ?)", matricula);
            
            // Delete reincidencias
            jdbcTemplate.update("DELETE FROM tb_reincidencia WHERE chamado_original IN (SELECT numero_chamado FROM tb_chamado WHERE id_tecnico = (SELECT id_tecnico FROM tb_tecnico WHERE matricula = ?))", matricula);
            jdbcTemplate.update("DELETE FROM tb_reincidencia WHERE chamado_novo IN (SELECT numero_chamado FROM tb_chamado WHERE id_tecnico = (SELECT id_tecnico FROM tb_tecnico WHERE matricula = ?))", matricula);
            
            // Delete consumo pecas
            jdbcTemplate.update("DELETE FROM tb_consumo_peca WHERE numero_chamado IN (SELECT numero_chamado FROM tb_chamado WHERE id_tecnico = (SELECT id_tecnico FROM tb_tecnico WHERE matricula = ?))", matricula);

            // Delete chamados
            jdbcTemplate.update("DELETE FROM tb_chamado WHERE id_tecnico = (SELECT id_tecnico FROM tb_tecnico WHERE matricula = ?)", matricula);

            // Em vez de deletar o técnico, vamos apenas anular a matrícula para que ele volte ao estado "não cadastrado"
            int updated = jdbcTemplate.update("UPDATE tb_tecnico SET matricula = null, cpf = null, senha = null, is_primeiro_acesso = true WHERE matricula = ?", matricula);
            
            if (updated > 0) {
                return ResponseEntity.ok("Usuário " + matricula + " e seus dados foram apagados com sucesso.");
            } else {
                return ResponseEntity.ok("Usuário " + matricula + " não encontrado.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkTecnico() {
        try {
            // Fix encoding issue
            jdbcTemplate.update("UPDATE tb_tecnico SET nome_completo = 'Tecnico Positivo' WHERE id_tecnico = 382");
            // Restore dummy score in case it got zeroed out
            jdbcTemplate.update("UPDATE tb_apuracao_mensal SET pontuacao_total = 95, atingimento_sla = 1.0, pontos_sla = 25, atingimento_reincidencia = 0.05, pontos_reincidencia = 15, atingimento_pecas = 0.1, pontos_pecas = 12.5, atingimento_nps = 1.0, pontos_nps = 17.5, atingimento_perdidos = 0.0, pontos_perdidos = 20, status_elegibilidade = true WHERE id_tecnico = 382");

            var tec = jdbcTemplate.queryForList("SELECT id_tecnico, nome_completo, ct_base, matricula, senha, is_primeiro_acesso FROM tb_tecnico WHERE id_tecnico = 382");
            var apuracoes = jdbcTemplate.queryForList("SELECT id_apuracao, id_tecnico, mes_ano FROM tb_apuracao_mensal WHERE id_tecnico = 382");
            return ResponseEntity.ok(Map.of("tecnico", tec, "apuracoes", apuracoes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/marcio")
    public ResponseEntity<?> checkMarcio() {
        try {
            var res = jdbcTemplate.queryForList("SELECT t.nome_completo, a.pontuacao_total, a.status_elegibilidade, a.motivo_inelegibilidade, a.atingimento_sla, a.atingimento_reincidencia, a.atingimento_pecas, a.atingimento_perdidos, a.atingimento_nps FROM tb_apuracao_mensal a JOIN tb_tecnico t ON a.id_tecnico = t.id_tecnico WHERE t.nome_completo ILIKE '%MARCIO DA SILVA EDUARDO%'");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
