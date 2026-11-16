# FinCore Banking

Plataforma bancária de referência construída com arquitetura de microsserviços em **Java 21 + Spring Boot 3.3.6**. O sistema cobre autenticação com JWT, gestão de contas, depósitos, extrato, chaves PIX e transferências PIX processadas por uma **saga assíncrona com Apache Kafka**, além de análise de fraude em tempo real e notificações ao usuário.

> Documentação complementar: [SETUP.md](SETUP.md) — guia de configuração de variáveis de ambiente.

---

## Sumário

- [Visão geral](#visão-geral)
- [Arquitetura](#arquitetura)
- [Stack tecnológica](#stack-tecnológica)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Passo 1 — Obter o código](#passo-1--obter-o-código)
- [Passo 2 — Configurar as variáveis de ambiente](#passo-2--configurar-as-variáveis-de-ambiente)
- [Passo 3 — Executar com Docker Compose (recomendado)](#passo-3--executar-com-docker-compose-recomendado)
- [Passo 4 — Executar o frontend](#passo-4--executar-o-frontend)
- [Passo 5 — Validar a instalação](#passo-5--validar-a-instalação)
- [Execução manual (sem Docker para os serviços)](#execução-manual-sem-docker-para-os-serviços)
- [Referência da API](#referência-da-api)
- [Fluxo PIX](#fluxo-pix)
- [Regras de fraude](#regras-de-fraude)
- [Bancos de dados](#bancos-de-dados)
- [Tópicos Kafka](#tópicos-kafka)
- [Comandos úteis](#comandos-úteis)
- [Solução de problemas](#solução-de-problemas)
- [Autor](#autor)

---

## Visão geral

| Recurso | Descrição |
|---|---|
| Autenticação | Registro, login, refresh token, perfil, troca de senha e PIN de transação |
| Contas | Abertura de conta, consulta, depósito e extrato |
| PIX | Cadastro de chaves (CPF, e-mail, telefone e aleatória) e transferências |
| Mensageria | Saga PIX orquestrada por eventos Kafka, com idempotência |
| Fraude | Análise síncrona por eventos, com 4 regras de risco configuráveis |
| Notificações | Avisos de PIX concluído ou falho por usuário |
| Frontend | SPA React com dashboard, PIX, depósito, extrato e perfil |

---

## Arquitetura

```
┌───────────────────────────────────────────────────────────────┐
│           fincore-frontend (React + Vite) :5173                │
│                  Cliente (curl / Postman)                      │
└───────────────────────────┬───────────────────────────────────┘
                            │ HTTP REST (proxy Vite: /auth, /account, /transaction)
          ┌─────────────────┼─────────────────────┐
          ▼                 ▼                     ▼
   ┌─────────────┐   ┌─────────────┐    ┌─────────────────┐
   │auth-service │   │account-     │    │transaction-     │
   │   :8081     │   │service :8082│    │service :8083    │
   └─────────────┘   └─────────────┘    └────────┬────────┘
          │                 │                    │
          └─────────────────┘              Kafka │ pix-initiated
                    │                          │ pix-debit-requested
               PostgreSQL                      │ pix-completed
                                               │ pix-failed
                                     ┌─────────┴─────────┐
                                     ▼                   ▼
                              ┌────────────┐      ┌────────────┐
                              │notification│      │   fraud-   │
                              │ :8084      │      │  :8085     │
                              └────────────┘      └────────────┘
```

### Microsserviços

| Serviço | Porta | Responsabilidade | Banco |
|---|---|---|---|
| `auth-service` | 8081 | Registro, login, emissão/validação de JWT, perfil e PIN | `fincore_auth` |
| `account-service` | 8082 | Contas, depósitos, extrato e chaves PIX | `fincore_account` |
| `transaction-service` | 8083 | Iniciação e orquestração das transferências PIX | `fincore_txn` |
| `notification-service` | 8084 | Notificações geradas a partir de eventos | `fincore_notification` |
| `fraud-service` | 8085 | Análise de risco em tempo real | `fincore_fraud` |

Cada serviço possui banco isolado (database-per-service) e migrations versionadas com Flyway.

---

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Linguagem / runtime | Java 21 (build validado também com JDK 27) |
| Framework | Spring Boot 3.3.6, Spring Security, Spring Data JPA |
| Autenticação | JWT (jjwt 0.12.6) |
| Persistência | PostgreSQL 16 + Hibernate + Flyway |
| Mensageria | Apache Kafka 7.6 (Confluent), Zookeeper |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS |
| Infraestrutura | Docker, Docker Compose |

---

## Estrutura do repositório

```
fincore-bank/
├── auth-service/            # Autenticação e JWT
├── account-service/         # Contas, depósitos e chaves PIX
├── transaction-service/     # Transferências PIX
├── notification-service/    # Notificações
├── fraud-service/           # Análise de fraude
├── fincore-frontend/        # SPA React + Vite
├── docker/
│   └── postgres/init/       # Scripts executados na 1ª inicialização do Postgres
├── docker-compose.yml       # Infraestrutura + microsserviços
├── fix-permissions.sh       # Corrige ownership de schemas em volumes existentes
├── .env.example             # Template de variáveis de ambiente
├── SETUP.md                 # Guia detalhado de configuração
└── README.md
```

---

## Pré-requisitos

| Ferramenta | Versão mínima | Observação |
|---|---|---|
| Docker Desktop | 4.x | Inclui Docker Compose v2 |
| Java JDK | 21 | Necessário apenas para execução manual |
| Node.js | 18 (recomendado 20 LTS) | Necessário apenas para o frontend |
| Git | 2.x | — |

No Windows, recomenda-se o backend WSL2 do Docker Desktop. As portas publicadas pelo Compose ficam acessíveis em `localhost`.

### Portas utilizadas

| Porta | Serviço | Acesso |
|---|---|---|
| 5173 | Frontend (Vite) | `127.0.0.1` |
| 8081–8085 | Microsserviços | `127.0.0.1` |
| 5433 | PostgreSQL | `127.0.0.1` (mapeada para 5432 no container) |
| 9092 | Kafka (listener externo) | todas as interfaces |
| 2181 | Zookeeper | somente rede interna |
| 5050 | pgAdmin | somente com `--profile dev` |

> **Importante:** as portas 8081–8085 precisam estar livres. Serviços rodando na IDE ocupando essas portas impedem os containers de subir.

---

## Passo 1 — Obter o código

```bash
git clone https://github.com/gustulodo099-beep/fincore-bank.git
cd fincore-bank
```

---

## Passo 2 — Configurar as variáveis de ambiente

O Compose lê o arquivo `.env` na raiz do projeto.

### 2.1 Crie o arquivo

```bash
# Linux / macOS / Git Bash / WSL
cp .env.example .env
```

```powershell
# PowerShell
Copy-Item .env.example .env
```

### 2.2 Preencha os valores

| Variável | Obrigatória | Descrição |
|---|---|---|
| `POSTGRES_USER` | sim | Superusuário criado pelo container do Postgres |
| `POSTGRES_PASSWORD` | sim | Senha do superusuário |
| `DB_USERNAME` / `DB_PASSWORD` | sim | Credenciais usadas na execução manual (devem ser iguais às do Postgres) |
| `AUTH_DB_NAME` / `AUTH_DB_USER` / `AUTH_DB_PASSWORD` | sim | Banco e usuário do auth-service |
| `ACCOUNT_DB_*`, `TXN_DB_*`, `NOTIFICATION_DB_*`, `FRAUD_DB_*` | sim | Idem para os demais serviços |
| `JWT_SECRET` | sim | Chave HMAC-SHA256; os serviços não sobem sem ela |
| `JWT_EXPIRATION` / `JWT_REFRESH_EXPIRATION` | não | Validade em ms (padrão: 24h / 7 dias) |
| `KAFKA_BOOTSTRAP_SERVERS` | não | Padrão da execução manual: `localhost:9092` |
| `FRAUD_MAX_AMOUNT` | não | Limite de valor por transação (padrão: `5000.00`) |
| `FRAUD_MAX_DAILY_TRANSACTIONS` | não | Máximo de transações diárias (padrão: `10`) |
| `FRAUD_SUSPICIOUS_AMOUNT` | não | Limiar de valor suspeito (padrão: `1000.00`) |

### 2.3 Gere um JWT_SECRET

```bash
# Linux / macOS / WSL / Git Bash
openssl rand -hex 32
```

```powershell
# PowerShell
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
```

> O mesmo `JWT_SECRET` deve ser usado por todos os serviços, pois o auth-service assina e os demais validam os tokens.
>
> **Nunca versione o arquivo `.env`.** Ele já está no `.gitignore`; mantenha apenas o `.env.example` com placeholders.

---

## Passo 3 — Executar com Docker Compose (recomendado)

### 3.1 Suba a stack

```bash
docker compose up -d
```

Na primeira execução as imagens dos cinco serviços são construídas, o que pode levar alguns minutos. O Compose respeita a seguinte ordem de dependências:

1. `postgres`, `zookeeper` e `kafka`;
2. `auth-service` (somente após o Postgres reportar `healthy`);
3. `account-service`, `transaction-service`, `notification-service` e `fraud-service` (após `auth-service` ficar `healthy`).

Na primeira inicialização, o Postgres executa os scripts de `docker/postgres/init/` e cria automaticamente:

- os schemas `fincore_auth`, `fincore_account`, `fincore_txn`, `fincore_notification` e `fincore_fraud`;
- os usuários `auth_user`, `account_user`, `txn_user`, `notification_user` e `fraud_user`, cada um como owner do schema `public` do seu banco.

### 3.2 Acompanhe a inicialização

```bash
docker compose ps
```

Todos os serviços devem aparecer como `Up` e, após o período de health check, como `Up (healthy)`.

```bash
# Logs de um serviço específico
docker compose logs -f auth-service

# Logs da stack inteira
docker compose logs -f
```

### 3.3 Comandos do dia a dia

| Objetivo | Comando |
|---|---|
| Subir em segundo plano | `docker compose up -d` |
| Subir aguardando health check | `docker compose up -d --wait` |
| Parar sem remover dados | `docker compose down` |
| Reconstruir após alterar código | `docker compose up -d --build` |
| Remover tudo, inclusive o banco | `docker compose down -v` |
| Ver logs | `docker compose logs -f [serviço]` |

> `docker compose down -v` apaga o volume `fincore-postgres-data`. Use apenas quando quiser reinicializar o banco do zero.

### 3.4 pgAdmin (opcional)

```bash
docker compose --profile dev up -d
```

Interface em http://localhost:5050 — usuário `admin@fincore.com`, senha `pgAdmin#2024!`. Para conectar ao banco, use host `postgres`, porta `5432`.

---

## Passo 4 — Executar o frontend

O Compose não sobe o frontend. Com os serviços `healthy`:

```bash
cd fincore-frontend
npm install
npm run dev
```

Acesse **http://localhost:5173**. O proxy do Vite encaminha `/auth`, `/account` e `/transaction` para os serviços nas portas 8081, 8082 e 8083 respectivamente.

---

## Passo 5 — Validar a instalação

### 5.1 Health checks

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

Resposta esperada: `{"status":"UP"}`.

```powershell
# PowerShell
8081..8085 | ForEach-Object { "porta $_ -> " + (Invoke-RestMethod "http://127.0.0.1:$_/actuator/health").status }
```

### 5.2 Fluxo ponta a ponta via API

**1) Registre um usuário**

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "João Silva",
    "email": "joao@email.com",
    "password": "Senha@123",
    "cpf": "52998224725",
    "phone": "11987654321",
    "transactionPin": "1234"
  }'
```

> O CPF precisa ser válido, a senha deve atender à política de senha forte e o `transactionPin` deve ter exatamente 4 dígitos (armazenado com BCrypt).

**2) Faça login e obtenha o token**

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "joao@email.com", "password": "Senha@123" }'
```

A resposta contém `accessToken` e `refreshToken`.

```bash
# Linux / macOS / Git Bash
export TOKEN="<accessToken>"
```

```powershell
# PowerShell
$TOKEN = "<accessToken>"
```

**3) Crie a conta**

```bash
curl -X POST http://localhost:8082/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{ "accountType": "CHECKING", "ownerName": "João Silva", "ownerEmail": "joao@email.com" }'
```

**4) Faça um depósito**

```bash
curl -X POST http://localhost:8082/api/v1/accounts/{id}/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{ "amount": 500.00 }'
```

**5) Cadastre uma chave PIX**

```bash
curl -X POST http://localhost:8082/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{ "accountId": 1, "keyType": "EMAIL", "keyValue": "joao@email.com" }'
```

**6) Envie um PIX**

```bash
curl -X POST http://localhost:8083/api/v1/transactions/pix \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "senderAccountId": 1,
    "receiverKey": "maria@email.com",
    "amount": 100.00,
    "description": "Pagamento",
    "transactionPin": "1234"
  }'
```

**7) Acompanhe o resultado**

```bash
curl http://localhost:8083/api/v1/transactions/pix/{transactionId} \
  -H "Authorization: Bearer $TOKEN"
```

A transação passa de `PENDING` para `COMPLETED` (ou `FAILED`). A notificação correspondente pode ser consultada em `GET /api/v1/notifications`.

---

## Execução manual (sem Docker para os serviços)

Útil para desenvolvimento e debug na IDE.

### 1. Suba apenas a infraestrutura

```bash
docker compose up -d postgres zookeeper kafka
```

### 2. Ajuste a conexão dos serviços

As `application.properties` apontam para `localhost:5432`. Como o Compose publica o Postgres na porta **5433** do host, sobrescreva a URL ao rodar localmente:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/fincore_auth
SPRING_DATASOURCE_USERNAME=auth_user
SPRING_DATASOURCE_PASSWORD=<AUTH_DB_PASSWORD>
JWT_SECRET=<JWT_SECRET>
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

No PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/fincore_auth"
$env:SPRING_DATASOURCE_USERNAME="auth_user"
$env:SPRING_DATASOURCE_PASSWORD="<AUTH_DB_PASSWORD>"
$env:JWT_SECRET="<JWT_SECRET>"
$env:KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
```

Repita para cada serviço, alterando banco e usuário. No IntelliJ, use **Run > Edit Configurations > EnvFile** apontando para o `.env`.

### 3. Inicie os serviços na ordem correta

```bash
# 1º
cd auth-service && ./mvnw spring-boot:run

# 2º — em terminais separados
cd account-service      && ./mvnw spring-boot:run
cd transaction-service  && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd fraud-service        && ./mvnw spring-boot:run
```

No Windows, use `.\mvnw.cmd` em vez de `./mvnw`.

### 4. Corrigindo permissões em volumes já existentes

Se o volume do Postgres foi criado antes de um ajuste nos scripts de init, o Flyway pode falhar com `permission denied for table flyway_schema_history`. Rode:

```bash
bash fix-permissions.sh
```

O script transfere o ownership de schema, tabelas e sequences para o usuário de serviço de cada banco.

> Os scripts em `docker/postgres/init/` só rodam com o volume vazio. Após alterá-los, recrie o volume com `docker compose down -v` e suba novamente.

---

## Referência da API

### Auth Service (`:8081`)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Cadastro de usuário |
| `POST` | `/api/v1/auth/login` | Login e geração de JWT |
| `POST` | `/api/v1/auth/refresh` | Renovação do token |
| `GET` | `/api/v1/auth/me` | Perfil do usuário autenticado |
| `GET` | `/api/v1/auth/profile` | Perfil do usuário autenticado |
| `PUT` | `/api/v1/auth/profile` | Atualizar nome e telefone |
| `PUT` | `/api/v1/auth/password` | Alterar senha de acesso |
| `PUT` | `/api/v1/auth/transaction-pin` | Alterar PIN de transação |
| `POST` | `/api/v1/auth/transaction-pin/validate` | Validar PIN (uso interno do transaction-service) |

### Account Service (`:8082`)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/accounts` | Criar conta bancária |
| `GET` | `/api/v1/accounts/my` | Listar contas do usuário |
| `GET` | `/api/v1/accounts/{id}` | Buscar conta por ID |
| `POST` | `/api/v1/accounts/{id}/deposit` | Realizar depósito |
| `GET` | `/api/v1/accounts/{id}/statement` | Extrato da conta |
| `POST` | `/api/v1/pix-keys` | Cadastrar chave PIX |
| `GET` | `/api/v1/pix-keys` | Listar chaves PIX do usuário |
| `DELETE` | `/api/v1/pix-keys/{id}` | Remover chave PIX |

**Chaves PIX:** tipos aceitos são `CPF`, `EMAIL`, `PHONE` e `RANDOM`. O valor é normalizado e validado (CPF com dígitos verificadores, e-mail e telefone). Para `RANDOM`, o valor pode ser omitido e um UUID é gerado. Chaves são únicas globalmente por valor normalizado.

### Transaction Service (`:8083`)

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/transactions/pix` | Iniciar transferência PIX |
| `GET` | `/api/v1/transactions/pix/{transactionId}` | Consultar transação |
| `GET` | `/api/v1/transactions/pix/history/{accountId}` | Histórico de transações |

### Notification Service (`:8084`)

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/notifications` | Listar notificações |
| `GET` | `/api/v1/notifications/unread-count` | Contador de não lidas |
| `PUT` | `/api/v1/notifications/{id}/read` | Marcar como lida |

---

## Fluxo PIX

```
1. Cliente envia POST /api/v1/transactions/pix (chave PIX do destinatário + PIN de transação)
2. transaction-service valida o PIN no auth-service (repassando o JWT do usuário)
3. transaction-service persiste a transação e publica pix-initiated
4. fraud-service consome pix-initiated e analisa a transação em paralelo
5. transaction-service consome pix-initiated, valida e publica pix-debit-requested
6. account-service resolve a conta destinatária pela chave PIX e, atomicamente,
   debita o remetente e credita o destinatário (PIX_OUT / PIX_IN)
7. account-service publica pix-debit-confirmed (ou pix-debit-failed em caso de erro)
8. transaction-service consome o resultado e marca COMPLETED ou FAILED
9. transaction-service publica pix-completed (ou pix-failed)
10. notification-service consome e cria a notificação para o usuário
```

A entrega é **idempotente**: reentregas do mesmo `transactionId` não debitam a conta novamente. Em caso de falha, a transação local do account-service é revertida e o PIX é marcado como `FAILED`.

---

## Regras de fraude

O `fraud-service` avalia cada transação PIX com quatro regras:

| Regra | Condição | Severidade |
|---|---|---|
| `AMOUNT_EXCEEDED_LIMIT` | Valor acima de R$ 5.000 | ALTA |
| `SUSPICIOUS_AMOUNT` | Valor entre R$ 1.000 e R$ 5.000 | MÉDIA |
| `HIGH_FREQUENCY` | Mais de 10 transações no dia | CRÍTICA |
| `SUSPICIOUS_RECEIVER_KEY` | Chave PIX inicia com `00000` | ALTA |

Os limites são configuráveis por `FRAUD_MAX_AMOUNT`, `FRAUD_SUSPICIOUS_AMOUNT` e `FRAUD_MAX_DAILY_TRANSACTIONS`.

---

## Bancos de dados

| Banco | Serviço | Usuário de serviço |
|---|---|---|
| `fincore_auth` | auth-service | `auth_user` |
| `fincore_account` | account-service | `account_user` |
| `fincore_txn` | transaction-service | `txn_user` |
| `fincore_notification` | notification-service | `notification_user` |
| `fincore_fraud` | fraud-service | `fraud_user` |

Conexão a partir do host:

```bash
psql -h 127.0.0.1 -p 5433 -U postgres -d fincore_auth
```

Dentro da rede do Compose, os serviços usam `postgres:5432`.

---

## Tópicos Kafka

| Tópico | Produtor | Consumidores |
|---|---|---|
| `pix-initiated` | transaction-service | transaction-service, fraud-service |
| `pix-debit-requested` | transaction-service | account-service |
| `pix-debit-confirmed` | account-service | transaction-service |
| `pix-debit-failed` | account-service | transaction-service |
| `pix-completed` | transaction-service | notification-service |
| `pix-failed` | transaction-service | notification-service |
| `fraud-alert` | fraud-service | — |

Listar tópicos:

```bash
docker exec fincore-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## Comandos úteis

```bash
# Status e saúde
docker compose ps
docker compose top

# Logs
docker compose logs -f transaction-service
docker compose logs -f --tail 100 postgres

# Shell no banco
docker exec -it fincore-postgres psql -U postgres -d fincore_txn

# Reiniciar um serviço após mudança de código
docker compose up -d --build account-service

# Validar a configuração do Compose
docker compose config

# Teste de build de um serviço
cd auth-service && ./mvnw clean test
```

---

## Solução de problemas

### `container fincore-postgres is unhealthy` / `dependency failed to start`

Verifique os logs de inicialização:

```bash
docker compose logs postgres
```

Causas comuns:

| Sintoma no log | Causa | Solução |
|---|---|---|
| `role "fincore_admin" does not exist` | Script de init usando superusuário fixo diferente do `POSTGRES_USER` | Mantenha o script usando `POSTGRES_USER` (já corrigido) e recrie o volume |
| `database "fincore_xxx" already exists` | Volume reutilizado com init antigo | `docker compose down -v` e suba novamente |
| `password authentication failed` | Senha do `.env` diferente da gravada no volume | Recrie o volume ou alinhe as senhas |

Após alterar qualquer arquivo em `docker/postgres/init/`:

```bash
docker compose down -v
docker compose up -d
```

### `ports are not available ... bind: An attempt was made to access a socket in a way forbidden by its access permissions`

A porta está ocupada ou reservada pelo sistema. Identifique o processo:

```powershell
netstat -ano | Select-String ':8081'
Get-Process -Id <PID>
```

Encerre o processo que ocupa a porta (por exemplo, um serviço rodando na IDE) ou ajuste o mapeamento no `docker-compose.yml`.

Para conferir faixas reservadas pelo Hyper-V/WinNAT no Windows:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

### Serviços reiniciando em loop

```bash
docker compose logs --tail 200 auth-service
```

As causas mais frequentes são `JWT_SECRET` ausente no `.env`, credenciais de banco incorretas ou tentativa de conexão ao Kafka antes de o broker estar pronto (o Compose já ordena as dependências, mas o broker pode levar alguns segundos adicionais).

### `permission denied for table flyway_schema_history`

```bash
bash fix-permissions.sh
```

### Frontend não alcança a API

Confirme que os serviços respondem em `127.0.0.1:8081`–`8085` e que o proxy do Vite (`fincore-frontend/vite.config.ts`) aponta para as portas corretas.

---

## Autor

**Gustavo** — [GitHub](https://github.com/gustulodo099-beep)

ADS — FATEC Mogi das Cruzes
