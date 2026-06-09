ALTER TABLE tb_apuracao_mensal 
ADD COLUMN atingimento_perdidos DECIMAL(19,4),
ADD COLUMN pontos_perdidos DOUBLE PRECISION;

ALTER TABLE tb_chamado
ADD COLUMN pp INTEGER DEFAULT 0,
ADD COLUMN status_chamado VARCHAR(255);
