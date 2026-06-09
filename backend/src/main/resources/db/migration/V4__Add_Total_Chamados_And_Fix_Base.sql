-- V4__Add_Total_Chamados_And_Fix_Base.sql
-- 1. Corrige o banco adicionando a coluna ausente para contagem da produtividade na Apuração Mensal
ALTER TABLE tb_apuracao_mensal ADD COLUMN total_chamados INT DEFAULT 0;

-- 2. Corrige a base do tecnico Márcio para RJ conforme solicitado
UPDATE tb_tecnico SET ct_base = '8789471' WHERE matricula_cpf = '72916';
