DROP VIEW IF EXISTS vw_ranking_mensal;

ALTER TABLE tb_apuracao_mensal ALTER COLUMN pontos_sla TYPE DECIMAL(6,2);
ALTER TABLE tb_apuracao_mensal ALTER COLUMN pontos_reincidencia TYPE DECIMAL(6,2);
ALTER TABLE tb_apuracao_mensal ALTER COLUMN pontos_pecas TYPE DECIMAL(6,2);
ALTER TABLE tb_apuracao_mensal ALTER COLUMN pontos_nps TYPE DECIMAL(6,2);

CREATE OR REPLACE VIEW vw_ranking_mensal AS
SELECT 
    RANK() OVER (ORDER BY a.pontuacao_total DESC) AS "Posição",
    t.nome_completo AS "Técnico",
    a.pontuacao_total AS "Nota Final",
    a.atingimento_sla * 100 AS "% SLA",
    a.pontos_sla AS "Pts SLA",
    a.status_elegibilidade AS "Elegível?",
    a.motivo_inelegibilidade AS "Motivo Inelegibilidade",
    a.mes_ano AS "Mês de Referência"
FROM tb_apuracao_mensal a
JOIN tb_tecnico t ON a.id_tecnico = t.id_tecnico;
