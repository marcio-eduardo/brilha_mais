import pandas as pd
from sqlalchemy import create_engine, text

engine = create_engine("postgresql://postgres.scdkwhbvaiekcusayetn:Brilha_mais@aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require")

try:
    with engine.begin() as conn:
        print("Tentando excluir os 160 técnicos inativos...")
        
        # Deleta as apurações relacionadas a esses técnicos inativos
        # Caso o usuário queira excluir o técnico, o histórico dele de apuração mensal também deve sumir.
        conn.execute(text("""
            DELETE FROM tb_apuracao_mensal 
            WHERE id_tecnico IN (SELECT id_tecnico FROM tb_tecnico WHERE ativo = false)
        """))
        
        # O mesmo para reincidência, consumo de peças e nps, se houver tabelas vinculadas
        # Se houver tabelas de chamados que dependem de tb_tecnico (FK), elas também bloqueariam.
        # Mas pelo schema, normalmente tb_apuracao_mensal é a principal com FK para tecnico.
        
        # Finalmente, deleta os técnicos
        result = conn.execute(text("DELETE FROM tb_tecnico WHERE ativo = false"))
        print(f"Exclusão concluída! {result.rowcount} técnicos foram permanentemente removidos do banco.")
        
except Exception as e:
    print("Erro durante a exclusão:", e)
