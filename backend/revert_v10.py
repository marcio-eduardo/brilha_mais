from sqlalchemy import create_engine, text

engine = create_engine("postgresql://postgres.scdkwhbvaiekcusayetn:Brilha_mais@aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require")

try:
    with engine.begin() as conn:
        print("Restoring tb_apuracao_mensal to V9 state...")
        
        # Drop the view since it depends on the columns
        conn.execute(text("DROP VIEW IF EXISTS vw_ranking_mensal CASCADE;"))
        
        # 1. Drop the V10 columns if they exist
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS atingimento_performance_equipe CASCADE;"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS pontos_performance_equipe CASCADE;"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS atingimento_reincidencia_equipe CASCADE;"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS pontos_reincidencia_equipe CASCADE;"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS atingimento_reincidencia_indiv CASCADE;"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal DROP COLUMN IF EXISTS pontos_reincidencia_indiv CASCADE;"))
        
        # 2. Re-add the V9 columns if they don't exist
        conn.execute(text("ALTER TABLE tb_apuracao_mensal ADD COLUMN IF NOT EXISTS atingimento_reincidencia DECIMAL(10,4);"))
        conn.execute(text("ALTER TABLE tb_apuracao_mensal ADD COLUMN IF NOT EXISTS pontos_reincidencia DECIMAL(6,2);"))
        
        # 3. Drop V10 from flyway_schema_history
        conn.execute(text("DELETE FROM flyway_schema_history WHERE version = '10';"))
        
        # 4. Recreate the view as it was in V9
        conn.execute(text("""
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
        """))
        
        print("Rollback to V9 completed successfully.")
        
except Exception as e:
    print("Error during rollback:", e)
