#!/bin/bash
# ============================================================
# FinCore Banking — Correção de Permissões PostgreSQL
# Resolve "permission denied for table flyway_schema_history"
#
# O que faz por banco:
#   1. Torna o usuário de serviço OWNER do schema public
#   2. Transfere ownership de TODAS as tabelas existentes
#   3. Transfere ownership de TODAS as sequences existentes
#   4. GRANT ALL em qualquer objeto restante
#
# Uso: bash fix-permissions.sh
# ============================================================
set -e

PG_ADMIN="fincore_admin"

fix_database() {
  local db="$1"
  local svc_user="$2"

  echo ""
  echo "── ${db} → ${svc_user} ──────────────────────────────"

  # 1. Torna o usuário dono do schema public (resolve CREATE futuro)
  docker exec fincore-postgres psql -U "$PG_ADMIN" -d "$db" -q -c \
    "ALTER SCHEMA public OWNER TO ${svc_user};"
  echo "  ✓ SCHEMA public owner → ${svc_user}"

  # 2. Transfere ownership de todas as tabelas já existentes
  #    (inclui flyway_schema_history criada por fincore_admin)
  docker exec fincore-postgres psql -U "$PG_ADMIN" -d "$db" -q -c "
    DO \$\$
    DECLARE r RECORD;
    BEGIN
      FOR r IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO ${svc_user}', r.tablename);
      END LOOP;
    END \$\$;
  "
  echo "  ✓ Tabelas transferidas para ${svc_user}"

  # 3. Transfere ownership de sequences
  docker exec fincore-postgres psql -U "$PG_ADMIN" -d "$db" -q -c "
    DO \$\$
    DECLARE r RECORD;
    BEGIN
      FOR r IN SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema='public' LOOP
        EXECUTE format('ALTER SEQUENCE public.%I OWNER TO ${svc_user}', r.sequence_name);
      END LOOP;
    END \$\$;
  "
  echo "  ✓ Sequences transferidas para ${svc_user}"

  # 4. GRANT ALL como safety net em qualquer objeto restante
  docker exec fincore-postgres psql -U "$PG_ADMIN" -d "$db" -q -c "
    GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO ${svc_user};
    GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${svc_user};
    GRANT ALL PRIVILEGES ON SCHEMA public TO ${svc_user};
  "
  echo "  ✓ GRANT ALL aplicado"

  # 5. Confirma que flyway_schema_history tem o owner correto
  local flyway_owner
  flyway_owner=$(docker exec fincore-postgres psql -U "$PG_ADMIN" -d "$db" -tAc \
    "SELECT tableowner FROM pg_tables WHERE tablename='flyway_schema_history' AND schemaname='public';" 2>/dev/null)
  if [ -n "$flyway_owner" ]; then
    echo "  ✓ flyway_schema_history owner: ${flyway_owner}"
  else
    echo "  ~ flyway_schema_history ainda não existe (será criada pelo Flyway)"
  fi
}

echo "============================================================"
echo " FinCore — Corrigindo permissões em todos os bancos"
echo "============================================================"

fix_database fincore_auth         auth_user
fix_database fincore_account      account_user
fix_database fincore_txn          txn_user
fix_database fincore_notification notification_user
fix_database fincore_fraud        fraud_user

echo ""
echo "============================================================"
echo " ✓ Permissões corrigidas em todos os bancos"
echo "============================================================"
