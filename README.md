# Brilha Mais ✨

O **Brilha Mais** é um sistema corporativo e gamificado desenvolvido para acompanhamento de desempenho técnico, visualização de metas (SLA, Reincidência, Perdas) e gestão de campanhas de incentivo para as equipes de atendimento e suporte.

O sistema processa chamados e métricas diretamente na base de dados, permitindo que os técnicos visualizem de forma transparente como o seu trabalho reflete na pontuação geral da equipe e de sua regional.

## 🛠️ Tecnologias Utilizadas

- **Frontend:** React + TypeScript, Vite, TailwindCSS, Zustand (State Management), Recharts (Gráficos)
- **Backend:** Java 21, Spring Boot, Spring Security (JWT), Spring Data JPA
- **Banco de Dados:** PostgreSQL (Hospedado via Supabase)
- **Infraestrutura:** Docker & Docker Compose
- **Data Ingestion (Scripts):** Python & Pandas (Leitura e processamento de planilhas operacionais)

---

## 🚀 Como iniciar o projeto localmente

Você tem duas formas principais de iniciar o projeto: via **Docker** (para rodar tudo de uma vez) ou **Manualmente** (ideal para desenvolvimento).

### Opção 1: Via Docker Compose (Mais fácil)

Se você possuir o Docker instalado na sua máquina, basta rodar o comando abaixo na raiz do projeto:

```bash
docker-compose up --build -d
```
*Isso vai iniciar o Banco de Dados, o Backend e o Frontend.*
Acesse o Frontend em: `http://localhost:3000`
Acesse a API do Backend em: `http://localhost:8080`

### Opção 2: Iniciando Manualmente (Modo Desenvolvimento)

#### Pré-requisitos:
- Java 21
- Node.js (v18+)
- Banco de Dados PostgreSQL configurado

#### 1. Iniciando o Backend (Spring Boot)
Navegue até a pasta do backend:
```bash
cd backend
```
Caso use o Maven Wrapper:
```bash
# No Windows
mvnw.cmd spring-boot:run

# No Linux/Mac
./mvnw spring-boot:run
```

#### 2. Iniciando o Frontend (React + Vite)
Abra uma **nova aba** no seu terminal e navegue para a pasta do frontend:
```bash
cd frontend
```

Instale as dependências na primeira vez:
```bash
npm install
```

Inicie o servidor de desenvolvimento:
```bash
npm run dev
```
O frontend estará acessível em `http://localhost:5173` (porta padrão do Vite) ou `http://localhost:3000`.

---
