from sqlalchemy import create_engine, text
engine = create_engine("postgresql://postgres.scdkwhbvaiekcusayetn:Brilha_mais@aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require")
try:
    with engine.connect() as conn:
        res = conn.execute(text("SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'tb_apuracao_mensal'"))
        for row in res:
            print(row)
except Exception as e:
    print("Error:", e)
