#!/bin/bash
# ============================================================
# FinCore Banking — Init PostgreSQL
# Script IDEMPOTENTE: verifica existência antes de criar.
# Executa na primeira inicialização do container (volume vazio).
# Para volumes existentes use o comando manual no README.
# ============================================================
set -e

PG_USER="${POSTGRES_USER:-fincore_admin}"
PG_PASS="${POSTGRES_PASSWORD:-fincore123}"

log()  { echo "[init] $*"; }
psql_c() { psql -v ON_ERROR_STOP=1 --username "$PG_USER" -d postgres -c "$1" -q; }
psql_t() { psql -v ON_ERROR_STOP=1 --username "$PG_USER" -d postgres -tAc "$1"; }

# ── Atualiza senha do admin ──────────────────────────────────
log "Configurando fincore_admin..."
psql_c "ALTER USER fincore_admin WITH PASSWORD '${PG_PASS}';"

# ── Cria banco se não existir ────────────────────────────────
create_db() {
  local db="$1" owner="$2"
  local exists; exists=$(psql_t "SELECT 1 FROM pg_database WHERE datname='${db}';")
  if [ "$exists" = "1" ]; then
    log "Banco '${db}' já existe — ignorando"
  else
    psql_c "CREATE DATABASE ${db} OWNER ${owner};"
    log "Banco criado: ${db}"
  fi
}

create_db fincore_auth         fincore_admin
create_db fincore_account      fincore_admin
create_db fincore_txn          fincore_admin
create_db fincore_notification fincore_admin
create_db fincore_fraud        fincore_admin

# ── Cria usuário de serviço se não existir ───────────────────
create_user() {
  local user="$1" pass="$2" connlimit="${3:-20}"
  local exists; exists=$(psql_t "SELECT 1 FROM pg_roles WHERE rolname='${user}';")
  if [ "$exists" = "1" ]; then
    psql_c "ALTER USER ${user} WITH PASSWORD '${pass}' CONNECTION LIMIT ${connlimit};"
    log "Usuário '${user}' atualizado"
  else
    psql_c "CREATE USER ${user} WITH PASSWORD '${pass}' CONNECTION LIMIT ${connlimit};"
    log "Usuário criado: ${user}"
  fi
}

create_user auth_user         "${AUTH_DB_PASSWORD:-$PG_PASS}" 20
create_user account_user      "${ACCOUNT_DB_PASSWORD:-$PG_PASS}" 20
create_user txn_user          "${TXN_DB_PASSWORD:-$PG_PASS}" 20
create_user notification_user "${NOTIFICATION_DB_PASSWORD:-$PG_PASS}" 10
create_user fraud_user        "${FRAUD_DB_PASSWORD:-$PG_PASS}" 10

# ── GRANT CONNECT ────────────────────────────────────────────
declare -A DB_USER=(
  [fincore_auth]=auth_user
  [fincore_account]=account_user
  [fincore_txn]=txn_user
  [fincore_notification]=notification_user
  [fincore_fraud]=fraud_user
)
for db in "${!DB_USER[@]}"; do
  user="${DB_USER[$db]}"
  psql_c "GRANT CONNECT ON DATABASE ${db} TO ${user};"
done

log "✓ Bancos e usuários prontos"
