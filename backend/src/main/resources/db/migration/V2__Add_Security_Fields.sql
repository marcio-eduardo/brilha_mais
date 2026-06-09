-- =================================================================================
-- V2: ADIÇÃO DE CAMPOS DE SEGURANÇA PARA AUTENTICAÇÃO (JWT)
-- =================================================================================

-- 1. Adicionando campos de senha e controle de primeiro acesso
ALTER TABLE tb_tecnico
ADD COLUMN senha VARCHAR(255),
ADD COLUMN is_primeiro_acesso BOOLEAN DEFAULT TRUE;

-- Nota: Como a matrícula (matricula_cpf) será fornecida posteriormente pelos gestores,
-- a senha inicial será definida no momento em que a matrícula for cadastrada/atualizada 
-- pelo sistema de gestão (via backend) utilizando BCrypt.
