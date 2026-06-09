-- V5__Rename_Matricula_And_Add_CPF.sql

-- 1. Renomear a coluna matricula_cpf para matricula na tabela tb_tecnico
ALTER TABLE tb_tecnico RENAME COLUMN matricula_cpf TO matricula;

-- 2. Adicionar a coluna cpf (VARCHAR)
ALTER TABLE tb_tecnico ADD COLUMN cpf VARCHAR(20) UNIQUE;

-- NOTA: O Flyway vai executar este script automaticamente no startup do Spring Boot.
