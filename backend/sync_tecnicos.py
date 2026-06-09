import pandas as pd
from sqlalchemy import create_engine, text

# Conexão com Supabase
engine = create_engine("postgresql://postgres.scdkwhbvaiekcusayetn:Brilha_mais@aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require")

file_path = r'c:\Users\marci\Documents\Positivo\brilha-mais\Project_Docs\Planilhas\Técnicos.xlsx'

print("Iniciando sincronização de técnicos...")

try:
    df = pd.read_excel(file_path)
    
    # Localizar a linha do cabeçalho
    header_row_idx = None
    for idx, row in df.iterrows():
        if 'Nome' in str(row.values) or 'NOME' in str(row.values).upper():
            header_row_idx = idx
            break
            
    if header_row_idx is not None:
        df.columns = df.iloc[header_row_idx]
        df = df.iloc[header_row_idx + 1:]
    else:
        print("Cabeçalho não encontrado!")
        exit(1)
        
    df = df.dropna(subset=['Nome'])
    
    col_nome = 'Nome'
    col_mat = [c for c in df.columns if 'Matr' in str(c)][0]

    with engine.begin() as conn:
        # Clear matricula to avoid unique constraint violations
        conn.execute(text("UPDATE tb_tecnico SET matricula = NULL"))
        
        res = conn.execute(text("SELECT id_tecnico, nome_completo FROM tb_tecnico"))
        banco_tecnicos = {row[1].strip().upper(): row[0] for row in res}
        
        planilha_nomes = set()
        updates = 0
        inserts = 0
        
        for idx, row in df.iterrows():
            nome = str(row[col_nome]).strip().upper()
            if not nome or nome == 'NAN':
                continue
                
            planilha_nomes.add(nome)
            
            matricula = str(row[col_mat]).strip()
            if matricula == 'nan' or matricula == 'None':
                matricula = None
            else:
                try:
                    matricula = str(int(float(matricula)))
                except:
                    pass
                
            if nome in banco_tecnicos:
                id_tec = banco_tecnicos[nome]
                conn.execute(text("""
                    UPDATE tb_tecnico 
                    SET matricula = :mat, ativo = true
                    WHERE id_tecnico = :id
                """), {"mat": matricula, "id": id_tec})
                updates += 1
            else:
                senha_padrao = '$2a$10$AVQwCGgg9CtZSwjvyWBazukr0R.zobLKR5bVObAk4xGn25dBPDxqK'
                conn.execute(text("""
                    INSERT INTO tb_tecnico (matricula, nome_completo, cargo, ativo, is_primeiro_acesso, senha)
                    VALUES (:mat, :nome, 'Técnico On-site', true, true, :senha)
                """), {"mat": matricula, "nome": nome, "senha": senha_padrao})
                inserts += 1
                
        desativados = 0
        for nome_banco, id_tec in banco_tecnicos.items():
            if nome_banco not in planilha_nomes:
                conn.execute(text("""
                    UPDATE tb_tecnico SET ativo = false WHERE id_tecnico = :id
                """), {"id": id_tec})
                desativados += 1
                
        print(f"Sincronização concluída com sucesso!")
        print(f"-> {inserts} técnicos NOVOS inseridos.")
        print(f"-> {updates} técnicos ATUALIZADOS e mantidos ativos.")
        print(f"-> {desativados} técnicos antigos DESATIVADOS (não estão na planilha).")
        
except Exception as e:
    print("Erro durante a sincronização:", e)
