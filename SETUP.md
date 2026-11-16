# Configuração do Ambiente Local — FinCore Banking System

Guia passo a passo para rodar todos os 5 microsserviços localmente.

---

## Pré-requisitos

| Ferramenta | Versão mínima | Para quê |
|---|---|---|
| Java (JDK) | 21 | Compilar e executar os serviços |
| Maven | 3.9+ (ou use o wrapper `./mvnw`) | Build dos projetos |
| Docker Desktop | 4.x | PostgreSQL + Kafka via container |
| Git | qualquer | Clonar o repositório |

> **Windows:** instale o Docker Desktop com WSL2 habilitado. Os comandos abaixo funcionam tanto no PowerShell quanto no terminal WSL.

---

## 1. Clone o repositório

```bash
git clone https://github.com/gustulodo099-beep/fincore-bank.git
cd fincore-bank
```

---

## 2. Configure as variáveis de ambiente

### 2.1 Copie o template

```bash
cp .env.example .env
```

### 2.2 Edite o `.env`

Abra o arquivo `.env` e preencha os campos marcados com `<CHANGE_ME>`.

**Variáveis obrigatórias para rodar localmente:**

| Variável | O que é | Exemplo local |
|---|---|---|
| `DB_PASSWORD` | Senha do PostgreSQL (usuário `fincore_admin`) | `fincore123` |
| `JWT_SECRET` | Secret HMAC-SHA256 para assinar tokens JWT | gere com `openssl rand -hex 32` |

**Como gerar um `JWT_SECRET` seguro:**

```bash
# Linux / macOS / WSL
openssl rand -hex 32

# PowerShell (Windows nativo)
-join ((1..32) | ForEach-Object { '{0:x2}' -f (Get-Random -Max 256) })
```

> O mesmo `JWT_SECRET` deve ser usado em todos os serviços — eles precisam validar tokens emitidos pelo auth-service.

### 2.3 Exemplo de `.env` completo para ambiente local

```dotenv
DB_USERNAME=fincore_admin
DB_PASSWORD=fincore123

POSTGRES_USER=fincore_admin
POSTGRES_PASSWORD=fincore123

AUTH_DB_NAME=fincore_auth
AUTH_DB_USER=auth_user
AUTH_DB_PASSWORD=fincore123

ACCOUNT_DB_NAME=fincore_account
ACCOUNT_DB_USER=account_user
ACCOUNT_DB_PASSWORD=fincore123

TXN_DB_NAME=fincore_txn
TXN_DB_USER=txn_user
TXN_DB_PASSWORD=fincore123

NOTIFICATION_DB_NAME=fincore_notification
NOTIFICATION_DB_USER=notification_user
NOTIFICATION_DB_PASSWORD=fincore123

FRAUD_DB_NAME=fincore_fraud
FRAUD_DB_USER=fraud_user
FRAUD_DB_PASSWORD=fincore123

JWT_SECRET=8f4b2e9d1c7a3f6e0b5d8a2c4e7f1b3d6a9c2e5f8b1d4a7e0c3f6b9d2a5e8c1f
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

KAFKA_BOOTSTRAP_SERVERS=localhost:9092

PGBOUNCER_MAX_CLIENT_CONN=100
PGBOUNCER_DEFAULT_POOL_SIZE=20

FRAUD_MAX_AMOUNT=5000.00
FRAUD_MAX_DAILY_TRANSACTIONS=10
FRAUD_SUSPICIOUS_AMOUNT=1000.00
```

---

## 3. Suba a infraestrutura com Docker

```bash
# Sobe PostgreSQL + Zookeeper + Kafka em background
docker compose up -d postgres zookeeper kafka

# Verifique que estão saudáveis
docker compose ps
```

O PostgreSQL já executa o script `docker/postgres/init/01_init_databases.sh` (+ `02_auth_permissions.sql`) que cria automaticamente os 5 bancos e os usuários de serviço:

```
fincore_auth        → auth-service        (auth_user)
fincore_account     → account-service     (account_user)
fincore_txn         → transaction-service (txn_user)
fincore_notification → notification-service (notification_user)
fincore_fraud       → fraud-service       (fraud_user)
```

> Os nomes de usuário são fixos; as senhas vêm de `<SERVICO>_DB_PASSWORD` no `.env`
> (fallback: `POSTGRES_PASSWORD`). Mantenha-as iguais às usadas pelos serviços.

> **Aguarde ~20 segundos** até o health check do PostgreSQL ficar `healthy` antes de iniciar os serviços.

---

## 4. Como os serviços leem o `.env`

Os `application.properties` usam `${VARIAVEL:default}` — o Spring Boot resolve essas variáveis a partir do ambiente do processo (system environment). O arquivo `.env` **não é lido automaticamente** pelo Spring Boot; você precisa carregá-lo no ambiente de uma das formas abaixo.

### Opção A — IntelliJ IDEA (recomendado para desenvolvimento)

1. Instale o plugin **EnvFile**: `File > Settings > Plugins > pesquise "EnvFile"`
2. Em cada configuração de Run/Debug: `Run > Edit Configurations`
3. Aba **EnvFile** → marque "Enable EnvFile" → adicione o `.env` da raiz do projeto
4. Repita para cada serviço (auth, account, transaction, notification, fraud)

### Opção B — Terminal (Linux / macOS / WSL)

```bash
# Exporta todas as variáveis do .env para o shell atual
set -a && source .env && set +a

# Em seguida, inicie cada serviço normalmente
cd auth-service && ./mvnw spring-boot:run
```

### Opção C — PowerShell (Windows nativo)

```powershell
# Carrega o .env no ambiente do PowerShell atual
Get-Content .env | Where-Object { $_ -notmatch '^#' -and $_ -match '=' } | ForEach-Object {
    $parts = $_ -split '=', 2
    [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), 'Process')
}

# Depois rode cada serviço
cd auth-service; ./mvnw spring-boot:run
```

### Opção D — Passar variáveis diretamente no comando Maven

```bash
JWT_SECRET=abc123... DB_PASSWORD=fincore123 KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  ./mvnw spring-boot:run
```

---

## 5. Inicie os microsserviços

Abra 5 terminais (ou use IntelliJ com múltiplos Run Configurations).

```bash
# Terminal 1 — auth-service (porta 8081)
cd auth-service && ./mvnw spring-boot:run

# Terminal 2 — account-service (porta 8082)
cd account-service && ./mvnw spring-boot:run

# Terminal 3 — transaction-service (porta 8083)
cd transaction-service && ./mvnw spring-boot:run

# Terminal 4 — notification-service (porta 8084)
cd notification-service && ./mvnw spring-boot:run

# Terminal 5 — fraud-service (porta 8085)
cd fraud-service && ./mvnw spring-boot:run
```

**Ordem recomendada:** auth → account → transaction → notification → fraud  
(o fraud e notification não dependem dos outros, mas a ordem acima evita mensagens de erro no log durante o boot)

---

## 6. Verifique que tudo está rodando

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
```

Todos devem retornar `{"status":"UP"}`.

---

## 7. Teste rápido do fluxo

### 7.1 Registre um usuário

```bash
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "João Silva",
    "email": "joao@fincore.com",
    "password": "Senha@123",
    "cpf": "52998224725",
    "phone": "11987654321",
    "transactionPin": "1234"
  }'
```

Guarde o `accessToken` da resposta. O CPF precisa ser válido, a senha deve atender à
política de senha forte e o `transactionPin` deve ter exatamente 4 dígitos.

### 7.2 Crie uma conta bancária

```bash
TOKEN=<accessToken_aqui>

curl -X POST http://localhost:8082/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "ownerName": "João Silva",
    "ownerEmail": "joao@fincore.com",
    "accountType": "CHECKING"
  }'
```

### 7.3 Faça um depósito

```bash
ACCOUNT_ID=<id_da_conta>

curl -X POST http://localhost:8082/api/v1/accounts/$ACCOUNT_ID/deposit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount": 500.00, "description": "Depósito inicial"}'
```

### 7.4 Cadastre uma chave PIX

```bash
curl -X POST http://localhost:8082/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "accountId": '"$ACCOUNT_ID"',
    "keyType": "EMAIL",
    "keyValue": "joao@fincore.com"
  }'
```

Tipos aceitos: `CPF`, `EMAIL`, `PHONE` e `RANDOM`. Para transferir, a chave do
destinatário também precisa estar cadastrada.

### 7.5 Inicie um PIX

```bash
curl -X POST http://localhost:8083/api/v1/transactions/pix \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "senderAccountId": '"$ACCOUNT_ID"',
    "receiverKey": "destino@email.com",
    "amount": 50.00,
    "description": "Teste PIX",
    "transactionPin": "1234"
  }'
```

O PIN é validado no auth-service antes de a transação ser criada. Consulte o resultado em
`GET /api/v1/transactions/pix/{transactionId}`.

### 7.6 Perfil e PIN de transação

```bash
# Consultar perfil
curl http://localhost:8081/api/v1/auth/me -H "Authorization: Bearer $TOKEN"

# Atualizar nome/telefone
curl -X PUT http://localhost:8081/api/v1/auth/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fullName": "João Silva", "phone": "11987654321"}'

# Alterar senha
curl -X PUT http://localhost:8081/api/v1/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currentPassword": "Senha@123", "newPassword": "NovaSenha@456"}'

# Alterar PIN de transação
curl -X PUT http://localhost:8081/api/v1/auth/transaction-pin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"currentPassword": "Senha@123", "newPin": "4321"}'
```

---

## 7.7 Rodar os testes automatizados

Os testes de auth-service e account-service usam PostgreSQL local (Flyway aplica as
migrations). Defina as variáveis antes de executar:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=<sua_senha>
export JWT_SECRET=$(openssl rand -hex 32)

cd auth-service && ./mvnw clean test
cd ../account-service && ./mvnw clean test
cd ../transaction-service && ./mvnw clean test
```

No Windows, use `.\mvnw.cmd clean test` e defina as variáveis com `$env:DB_USERNAME="postgres"`,
por exemplo. O build do frontend é validado com `cd fincore-frontend && npm run build`.

---

## 8. Frontend (React + Vite)

```bash
cd fincore-frontend
npm install
npm run dev
```

Disponível em http://localhost:5173. O proxy do Vite encaminha `/auth`, `/account`
e `/transaction` para os serviços nas portas 8081, 8082 e 8083.

Fluxo sugerido no navegador:

1. Crie uma conta em `/register` informando CPF válido, senha forte e PIN de 4 dígitos.
2. Em **Chaves PIX**, cadastre ao menos uma chave para a sua conta.
3. Use outra conta para enviar um PIX para essa chave; informe o PIN de transação.
4. Acompanhe o resultado em **Extrato** (PIX enviado/recebido) e no histórico da página PIX.
5. Em **Perfil** (avatar no topo ou bloco do usuário na sidebar), atualize seus dados,
   troque a senha de acesso e o PIN de transação.

---

## 9. Swagger / Documentação da API

Cada serviço expõe o Swagger UI:

| Serviço | URL |
|---|---|
| auth-service | http://localhost:8081/swagger-ui.html |
| account-service | http://localhost:8082/swagger-ui.html |
| transaction-service | http://localhost:8083/swagger-ui.html |
| notification-service | http://localhost:8084/swagger-ui.html |
| fraud-service | http://localhost:8085/swagger-ui.html |

---

## 10. Parar tudo

```bash
# Para os serviços Spring Boot: Ctrl+C em cada terminal

# Para a infraestrutura Docker
docker compose down

# Para remover os volumes (apaga dados do banco)
docker compose down -v
```

---

## Variáveis de Ambiente — Referência Completa

| Variável | Serviços que usam | Obrigatória | Default | Descrição |
|---|---|---|---|---|
| `JWT_SECRET` | todos | **sim** | — | Secret HMAC-SHA256 para JWT |
| `JWT_EXPIRATION` | auth | não | `86400000` | Validade do access token (ms) |
| `JWT_REFRESH_EXPIRATION` | auth | não | `604800000` | Validade do refresh token (ms) |
| `DB_USERNAME` | todos | não | `fincore_admin` | Usuário PostgreSQL |
| `DB_PASSWORD` | todos | **sim** | — | Senha PostgreSQL |
| `DB_URL` | todos | não | URL com localhost | URL JDBC do banco (override por serviço) |
| `KAFKA_BOOTSTRAP_SERVERS` | account, txn, notif, fraud | não | `localhost:9092` | Endereço do Kafka |
| `FRAUD_MAX_AMOUNT` | fraud | não | `5000.00` | Valor máximo por transação (R$) |
| `FRAUD_MAX_DAILY_TRANSACTIONS` | fraud | não | `10` | Máx. transações por dia |
| `FRAUD_SUSPICIOUS_AMOUNT` | fraud | não | `1000.00` | Valor a partir do qual monitora |
| `POSTGRES_USER` | docker-compose | docker | — | Superuser PostgreSQL |
| `POSTGRES_PASSWORD` | docker-compose | docker | — | Senha do superuser |
| `PGBOUNCER_MAX_CLIENT_CONN` | docker-compose | não | `100` | Conexões máx. PgBouncer |
| `PGBOUNCER_DEFAULT_POOL_SIZE` | docker-compose | não | `20` | Pool size PgBouncer |

---

## Resolução de Problemas

**`Failed to configure a DataSource` ao iniciar**
→ `DB_PASSWORD` não foi carregado no ambiente. Verifique se o `.env` está sendo lido (seção 4).

**`Connection refused` ao conectar no PostgreSQL**
→ O container PostgreSQL ainda não subiu. Rode `docker compose ps` e aguarde o status `healthy`.

**`Connection refused` ao conectar no Kafka**
→ Execute `docker compose up -d zookeeper kafka` e aguarde ~10 segundos.

**`OptimisticLockException` ao fazer depósitos simultâneos**
→ Comportamento esperado — o banco detectou concorrência e rejeitou uma das operações. A lógica de retry fica na camada do cliente.

**Porta em uso (`Address already in use`)**
→ Outro processo usa a porta 808x. Identifique com `netstat -an | grep 808` e encerre o processo.

**`JWT_SECRET` não configurado — erro `IllegalArgumentException`**
→ Sem o secret, o serviço recusa inicializar. Gere um secret com `openssl rand -hex 32` e adicione ao `.env`.
