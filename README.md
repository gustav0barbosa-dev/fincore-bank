# 🏦 FinCore Banking System

Sistema bancário baseado em microsserviços desenvolvido com Java 21 e Spring Boot 3.3.6, implementando operações financeiras como autenticação JWT, gerenciamento de contas e transferências PIX com processamento assíncrono via Apache Kafka.


---

## 📐 Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│              fincore-frontend (React + Vite) :5173           │
│                     Cliente (curl / Postman)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP REST (proxy Vite: /auth, /account, /transaction)
          ┌────────────────┼────────────────────┐
          ▼                ▼                    ▼
   ┌─────────────┐  ┌─────────────┐   ┌─────────────────┐
   │auth-service │  │account-     │   │transaction-     │
   │  :8081      │  │service :8082│   │service :8083    │
   └─────────────┘  └─────────────┘   └────────┬────────┘
          │                │                    │
          └────────────────┘              Kafka │ pix-initiated
                    │                           │ pix-completed
               PostgreSQL                       │ pix-failed
                                    ┌───────────┼───────────┐
                                    ▼           ▼           ▼
                             ┌──────────┐ ┌──────────┐ (fraud)
                             │notif-    │ │fraud-    │
                             │service   │ │service   │
                             │:8084     │ │:8085     │
                             └──────────┘ └──────────┘
```

### Microsserviços

| Serviço | Porta | Responsabilidade |
|---|---|---|
| **auth-service** | 8081 | Registro, login e validação JWT |
| **account-service** | 8082 | Criação de contas, depósitos e extratos |
| **transaction-service** | 8083 | Iniciação e processamento de PIX |
| **notification-service** | 8084 | Notificações de eventos financeiros |
| **fraud-service** | 8085 | Análise de fraude em tempo real |

---

## 🛠️ Stack Tecnológica

- **Java 21** (compilado e validado também com JDK 27) + **Spring Boot 3.3.6**
- **Spring Security** + **JWT** (jjwt 0.12.6)
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL 16** (banco de dados)
- **Apache Kafka** (mensageria assíncrona)
- **Flyway** (migrations de banco)
- **Lombok** (redução de boilerplate)
- **Docker** + **Docker Compose**

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21 (o build também funciona com JDK 27)
- Docker Desktop
- WSL2 (recomendado no Windows, para o backend do Docker)

### 1. Clone o repositório

```bash
git clone https://github.com/gustulodo099-beep/fincore-bank.git
cd fincore-bank
```

### 2. Configure as variáveis de ambiente

```bash
cp .env.example .env
# Edite o .env e preencha os valores (especialmente JWT_SECRET e DB_PASSWORD)
```

> Veja o guia completo em [SETUP.md](SETUP.md).

### 3a. Docker (recomendado)

```bash
cp .env.example .env
docker compose up -d
```

O compose sobe PostgreSQL, Kafka e os 5 microsserviços (portas 8081–8085).
Ele **não** sobe o frontend — após os serviços ficarem `healthy`, rode:

```bash
cd fincore-frontend
npm install
npm run dev
```

Acesse http://localhost:5173

### 3b. Manual (sem Docker)

#### Suba a infraestrutura (PostgreSQL + Kafka)

```bash
docker compose up -d postgres zookeeper kafka
```

O PostgreSQL do compose é publicado em `127.0.0.1:5432`, permitindo que os
serviços executados na máquina host conectem em `localhost:5432`.

### 4. Inicie os microsserviços (em terminais separados)

```bash
# auth-service
cd auth-service && ./mvnw spring-boot:run

# account-service
cd account-service && ./mvnw spring-boot:run

# transaction-service
cd transaction-service && ./mvnw spring-boot:run

# notification-service
cd notification-service && ./mvnw spring-boot:run

# fraud-service
cd fraud-service && ./mvnw spring-boot:run
```

> **Windows:** com Docker Desktop (backend WSL2 ou Hyper-V), as portas publicadas
> ficam acessíveis em `localhost`. Use JDK 21 ou superior (validado com JDK 27).
> **IntelliJ:** Configure o arquivo `.env` em Run > Edit Configurations > EnvFile.

---

## 📡 Endpoints

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

## 💻 Frontend

- **Stack:** React 18 + TypeScript + Vite + Tailwind CSS
- **Páginas:** Login, Cadastro, Dashboard, PIX, Chaves PIX, Depósito, Extrato e Perfil
- **Perfil:** edição de nome/telefone, troca de senha e troca do PIN de transação
- **Chaves PIX:** cadastro, listagem, cópia e remoção de chaves CPF, e-mail, telefone e aleatória
- **Validações no cliente:** CPF com dígitos verificadores, força de senha e formato de telefone

### Como rodar localmente

```bash
cd fincore-frontend
npm install
npm run dev
```

A aplicação fica disponível em http://localhost:5173, com o proxy do Vite encaminhando `/auth`, `/account` e `/transaction` para os respectivos microsserviços.

---

## 🔄 Fluxo PIX

```
1. Cliente envia POST /api/v1/transactions/pix (com a chave PIX do destinatário e o PIN de transação)
2. transaction-service valida o PIN no auth-service (repassando o JWT do usuário)
3. transaction-service persiste a transação e publica pix-initiated no Kafka
4. fraud-service consome pix-initiated e analisa a transação (paralelo)
5. transaction-service consome pix-initiated, valida e publica pix-debit-requested
6. account-service resolve a conta destinatária pela chave PIX e, de forma atômica, debita o
   remetente e credita o destinatário (PIX_OUT / PIX_IN)
7. account-service publica pix-debit-confirmed (ou pix-debit-failed em caso de erro)
8. transaction-service consome o resultado: marca COMPLETED ou FAILED
9. transaction-service publica pix-completed (ou pix-failed)
10. notification-service consome e cria notificação para o usuário
```

A entrega é idempotente: reentregas do mesmo `transactionId` não debitam a conta novamente.
Em caso de falha, a transação local do account-service é revertida e o PIX é marcado como `FAILED`.

---

## 🧪 Exemplos de Uso

### Registro de usuário

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

> `cpf` deve ser um CPF válido, `password` precisa atender à política de senha forte e
> `transactionPin` deve ter exatamente 4 dígitos. O PIN é armazenado com BCrypt.

### Criar conta

```bash
curl -X POST http://localhost:8082/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "accountType": "CHECKING",
    "ownerName": "João Silva",
    "ownerEmail": "joao@email.com"
  }'
```

### Cadastrar chave PIX

```bash
curl -X POST http://localhost:8082/api/v1/pix-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "accountId": 1,
    "keyType": "EMAIL",
    "keyValue": "joao@email.com"
  }'
```

Tipos aceitos: `CPF`, `EMAIL`, `PHONE` e `RANDOM`. A chave é normalizada e validada
(CPF com dígitos verificadores, e-mail e telefone). Para `RANDOM`, o valor pode ser omitido
e um UUID é gerado. Chaves são únicas globalmente por valor normalizado.

### Transferência PIX

```bash
curl -X POST http://localhost:8083/api/v1/transactions/pix \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "senderAccountId": 1,
    "receiverKey": "maria@email.com",
    "amount": 100.00,
    "description": "Pagamento",
    "transactionPin": "1234"
  }'
```

A chave do destinatário precisa estar cadastrada via `/api/v1/pix-keys`. O PIN é validado
no auth-service antes de a transação ser criada.

---

## 🛡️ Regras de Fraude

O **fraud-service** analisa cada transação PIX com 4 regras:

| Regra | Condição | Severidade |
|---|---|---|
| `AMOUNT_EXCEEDED_LIMIT` | Valor > R$ 5.000 | ALTA |
| `SUSPICIOUS_AMOUNT` | Valor entre R$ 1.000 e R$ 5.000 | MÉDIA |
| `HIGH_FREQUENCY` | Mais de 10 transações no dia | CRÍTICA |
| `SUSPICIOUS_RECEIVER_KEY` | Chave PIX começa com `00000` | ALTA |

---

## 🗄️ Bancos de Dados

| Banco | Serviço |
|---|---|
| `fincore_auth` | auth-service |
| `fincore_account` | account-service |
| `fincore_txn` | transaction-service |
| `fincore_notification` | notification-service |
| `fincore_fraud` | fraud-service |

---

## 📬 Tópicos Kafka

| Tópico | Produtor | Consumidores |
|---|---|---|
| `pix-initiated` | transaction-service | transaction-service, fraud-service |
| `pix-debit-requested` | transaction-service | account-service |
| `pix-debit-confirmed` | account-service | transaction-service |
| `pix-debit-failed` | account-service | transaction-service |
| `pix-completed` | transaction-service | notification-service |
| `pix-failed` | transaction-service | notification-service |
| `fraud-alert` | fraud-service | — |

---

## 👤 Autor

**Gustavo** — [GitHub](https://github.com/gustulodo099-beep)

ADS - FATEC Mogi das Cruzes
