-- =================================================================================
-- PROJETO BRILHA MAIS - SCRIPTS DE CRIAÇÃO DO BANCO DE DADOS (POSTGRESQL)
-- =================================================================================

-- ---------------------------------------------------------------------------------
-- 1. TABELAS DE CADASTRO (DIMENSÕES)
-- Origem: BASE ATPs.csv, Cidades.csv, Bairros SP.csv
-- ---------------------------------------------------------------------------------

-- Tabela de Bases/ATPs (Unidades de Operação)
CREATE TABLE tb_base_atp (
    ct_codigo VARCHAR(20) PRIMARY KEY, -- Ex: 2791005, 2791040
    nome_atp VARCHAR(150) NOT NULL,
    tipo_atp VARCHAR(50), -- PRÓPRIA SEM SP, FILIAL SP, TERCEIRA
    cidade VARCHAR(100),
    uf CHAR(2),
    regiao VARCHAR(50),
    supervisor VARCHAR(100),
    responsavel VARCHAR(100)
);

-- Tabela de Técnicos
CREATE TABLE tb_tecnico (
    id_tecnico SERIAL PRIMARY KEY,
    matricula_cpf VARCHAR(20) UNIQUE, -- Para login futuro
    nome_completo VARCHAR(150) NOT NULL,
    ct_base VARCHAR(20) REFERENCES tb_base_atp(ct_codigo),
    cargo VARCHAR(100) DEFAULT 'Técnico On-site',
    ativo BOOLEAN DEFAULT TRUE
);

-- ---------------------------------------------------------------------------------
-- 2. TABELAS OPERACIONAIS (DADOS BRUTOS / FATOS)
-- Origem: Base DL.csv, Consumo Peças.csv, Reincidências.csv
-- ---------------------------------------------------------------------------------

-- Tabela Central de Chamados (A base de tudo)
CREATE TABLE tb_chamado (
    numero_chamado BIGINT PRIMARY KEY, -- Ex: 60006363949
    id_tecnico INT REFERENCES tb_tecnico(id_tecnico),
    ct_base VARCHAR(20) REFERENCES tb_base_atp(ct_codigo),
    data_abertura TIMESTAMP NOT NULL,
    data_encerramento TIMESTAMP,
    segmento VARCHAR(50), -- Ex: PI-GOVERNO, PI-CORPORA
    equipamento VARCHAR(50), -- Ex: DESKTOP, NOTEBOOK, URNA
    projeto VARCHAR(50),
    status_sla VARCHAR(20), -- DENTRO, FORA
    tempo_atendimento_min INT,
    classificacao_chamado VARCHAR(100) -- Ex: SEM OCORRÊNCIA, TRANSFERENCIA ENTRE BASES
);

-- Tabela de Consumo de Peças
CREATE TABLE tb_consumo_peca (
    id_consumo SERIAL PRIMARY KEY,
    numero_chamado BIGINT REFERENCES tb_chamado(numero_chamado),
    codigo_peca VARCHAR(50),
    descricao_peca VARCHAR(255),
    grupo_mercadoria VARCHAR(100), -- Ex: HDD, SSD, PLM, LCD (crucial para o KPI de peças)
    quantidade INT DEFAULT 1
);

-- Tabela de Reincidências (Chamados reabertos em menos de 90 dias)
CREATE TABLE tb_reincidencia (
    id_reincidencia SERIAL PRIMARY KEY,
    chamado_original BIGINT REFERENCES tb_chamado(numero_chamado),
    chamado_novo BIGINT REFERENCES tb_chamado(numero_chamado),
    intervalo_dias INT,
    motivo_classificacao VARCHAR(150),
    responsavel_auditoria VARCHAR(100)
);

-- ---------------------------------------------------------------------------------
-- 3. MOTOR DE REGRAS DA CAMPANHA (CONFIGURAÇÕES)
-- Origem: Memoria Calculo.csv
-- Transformamos a planilha de regras em tabelas dinâmicas!
-- ---------------------------------------------------------------------------------

-- Tabela de Definição dos KPIs
CREATE TABLE tb_regra_kpi (
    id_regra SERIAL PRIMARY KEY,
    nome_indicador VARCHAR(100) NOT NULL,
    descricao TEXT,
    classe VARCHAR(20), -- EQUIPE ou INDIVIDUAL
    is_gatilho BOOLEAN DEFAULT FALSE,
    peso_percentual DECIMAL(5,2), -- Ex: 20.00, 12.50
    meta_percentual DECIMAL(5,2)  -- Ex: 0.90 (90%)
);

-- Tabela de Faixas de Pontuação (Substitui os "SEs" aninhados do Excel)
CREATE TABLE tb_faixa_pontuacao (
    id_faixa SERIAL PRIMARY KEY,
    id_regra INT REFERENCES tb_regra_kpi(id_regra),
    valor_minimo DECIMAL(5,4), -- Ex: 0.9000
    valor_maximo DECIMAL(5,4), -- Ex: 0.9999
    pontos_obtidos INT         -- Ex: 15, 20, 25
);

-- ---------------------------------------------------------------------------------
-- 4. CONSOLIDAÇÃO DE RESULTADOS (O QUE A API VAI MANDAR PRO APP)
-- Origem: RESULTADO BRILHA MAIS.csv
-- ---------------------------------------------------------------------------------

CREATE TABLE tb_apuracao_mensal (
    id_apuracao SERIAL PRIMARY KEY,
    id_tecnico INT REFERENCES tb_tecnico(id_tecnico),
    mes_ano DATE NOT NULL, -- Ex: '2026-05-01'
    
    -- Notas dos Indicadores
    atingimento_sla DECIMAL(5,4),
    pontos_sla INT,
    
    atingimento_reincidencia DECIMAL(5,4),
    pontos_reincidencia INT,
    
    atingimento_pecas DECIMAL(5,4),
    pontos_pecas INT,
    
    atingimento_nps DECIMAL(5,4),
    pontos_nps INT,
    
    -- Fechamento
    pontuacao_total DECIMAL(6,2),
    status_elegibilidade BOOLEAN DEFAULT TRUE,
    motivo_inelegibilidade VARCHAR(200),
    
    data_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------------
-- CARGA INICIAL DE EXEMPLO (MOTOR DE REGRAS)
-- Baseado na "Memoria Calculo.csv"
-- ---------------------------------------------------------------------------------

INSERT INTO tb_regra_kpi (nome_indicador, classe, is_gatilho, peso_percentual, meta_percentual) VALUES
('SLA chamado on site', 'EQUIPE', TRUE, 20.00, 0.90),
('Reincidência Técnica Equipe', 'EQUIPE', TRUE, 20.00, 0.07),
('Eficiência de Peças', 'INDIVIDUAL', FALSE, 12.50, 0.25);

-- Inserindo as faixas de pontuação para o SLA (Gatilho)
-- >= 100% (1.0) = 20 pontos | 90% a 99% = 15 pontos | < 90% = 0 pontos
INSERT INTO tb_faixa_pontuacao (id_regra, valor_minimo, valor_maximo, pontos_obtidos) VALUES
(1, 1.0000, 9.9999, 20),
(1, 0.9000, 0.9999, 15),
(1, 0.0000, 0.8999, 0);