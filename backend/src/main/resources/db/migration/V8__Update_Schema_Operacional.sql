-- =================================================================================
-- V8 - ATUALIZAÇÃO DO ESQUEMA PARA MÚLTIPLAS PLANILHAS OPERACIONAIS
-- =================================================================================

-- 1. A tabela tb_chamado já possui a coluna 'segmento' (criada na V1).
-- Vamos adicionar as colunas necessárias vindas da planilha SLA:
ALTER TABLE tb_chamado ADD COLUMN IF NOT EXISTS ofensor VARCHAR(255);
ALTER TABLE tb_chamado ADD COLUMN IF NOT EXISTS encdesc VARCHAR(255);

-- 2. Atualizar a tabela tb_reincidencia para alinhar com os nomes do Excel
ALTER TABLE tb_reincidencia RENAME COLUMN chamado_original TO chamado_anterior;
ALTER TABLE tb_reincidencia RENAME COLUMN chamado_novo TO chamado_rrc;

ALTER TABLE tb_reincidencia ADD COLUMN IF NOT EXISTS meses_rrc INT;
ALTER TABLE tb_reincidencia ADD COLUMN IF NOT EXISTS tecnico_anterior VARCHAR(150);

-- 3. Atualizar a View 3 que utilizava a coluna 'chamado_novo'
DROP VIEW IF EXISTS vw_reincidencia_por_tecnico;

CREATE VIEW vw_reincidencia_por_tecnico AS
SELECT 
    t.nome_completo AS "Técnico",
    COUNT(DISTINCT c.numero_chamado) AS "Total de Chamados Atendidos",
    COUNT(r.id_reincidencia) AS "Qtd Reincidências",
    ROUND(
        COUNT(r.id_reincidencia) * 100.0 / NULLIF(COUNT(DISTINCT c.numero_chamado), 0)
    , 2) AS "% Reincidência"
FROM tb_tecnico t
JOIN tb_chamado c ON t.id_tecnico = c.id_tecnico
LEFT JOIN tb_reincidencia r ON c.numero_chamado = r.chamado_rrc 
GROUP BY t.nome_completo;
