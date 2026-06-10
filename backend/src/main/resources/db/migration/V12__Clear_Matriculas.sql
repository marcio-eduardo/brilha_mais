-- V12__Clear_Matriculas.sql

-- 1. Remover todas as matrículas, exceto a do administrador (72916)
--    Isso força que todos os técnicos passem pelo novo fluxo de Primeiro Acesso via Modal.
UPDATE tb_tecnico
SET matricula = NULL, 
    senha = NULL, 
    is_primeiro_acesso = TRUE
WHERE matricula IS DISTINCT FROM '72916';
