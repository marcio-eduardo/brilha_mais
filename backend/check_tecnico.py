import pandas as pd
from sqlalchemy import create_engine, text

# 1. Inspect table
engine = create_engine("postgresql://postgres.scdkwhbvaiekcusayetn:Brilha_mais@aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require")
try:
    with engine.connect() as conn:
        print("COLUMNS IN TB_TECNICO:")
        res = conn.execute(text("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'tb_tecnico'"))
        for row in res:
            print(row)
            
        print("\nDATA EXAMPLES:")
        res2 = conn.execute(text("SELECT * FROM tb_tecnico LIMIT 2"))
        for row in res2:
            print(row)
except Exception as e:
    print("Error:", e)
