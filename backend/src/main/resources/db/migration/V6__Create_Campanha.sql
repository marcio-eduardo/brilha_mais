-- =================================================================================
-- V2: CRIAÇÃO DA TABELA DE CONFIGURAÇÃO DE CAMPANHA (BRILHA MAIS)
-- =================================================================================

CREATE TABLE tb_campanha (
    id_campanha SERIAL PRIMARY KEY,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    ativa BOOLEAN DEFAULT TRUE,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inserindo a campanha padrão inicial (Cobrindo todo o mês de Maio de 2026, baseado na carga atual)
INSERT INTO tb_campanha (data_inicio, data_fim, ativa) VALUES ('2026-05-01', '2026-05-31', TRUE);
