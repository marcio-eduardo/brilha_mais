-- Criação da tabela para armazenar o NPS dos chamados
CREATE TABLE IF NOT EXISTS tb_nps (
    id_nps SERIAL PRIMARY KEY,
    numero_chamado BIGINT NOT NULL,
    nota INTEGER NOT NULL,
    classificacao VARCHAR(50), -- 'PROMOTOR', 'NEUTRO', 'DETRATOR'
    CONSTRAINT fk_nps_chamado FOREIGN KEY (numero_chamado) REFERENCES tb_chamado(numero_chamado) ON DELETE CASCADE
);

-- Índices para melhorar o tempo de busca
CREATE INDEX IF NOT EXISTS idx_nps_chamado ON tb_nps(numero_chamado);
