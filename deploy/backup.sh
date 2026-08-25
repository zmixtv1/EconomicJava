#!/usr/bin/env bash
#
# Backup diário do banco. Guarda as últimas 7 cópias e apaga as mais velhas.
#
# Instalar no Raspberry (roda às 3h da manhã):
#   chmod +x deploy/backup.sh
#   crontab -e
#   0 3 * * * /home/pi/EconomicJava/deploy/backup.sh >> /home/pi/backup.log 2>&1
#
# Restaurar uma cópia:
#   gunzip -c ~/backups/despesas-2026-08-25.sql.gz | \
#     docker compose exec -T db psql -U despesas -d despesas

set -euo pipefail

PROJETO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DESTINO="${BACKUP_DIR:-$HOME/backups}"
MANTER="${BACKUP_MANTER:-7}"

cd "$PROJETO"

# Lê o nome do banco e o usuário do .env, com os mesmos padrões do compose.
BANCO="$(grep -E '^DATABASE_NAME=' .env 2>/dev/null | cut -d= -f2- || true)"
USUARIO="$(grep -E '^DATABASE_USER=' .env 2>/dev/null | cut -d= -f2- || true)"
BANCO="${BANCO:-despesas}"
USUARIO="${USUARIO:-despesas}"

mkdir -p "$DESTINO"
ARQUIVO="$DESTINO/${BANCO}-$(date +%F).sql.gz"

echo "[$(date +'%F %T')] iniciando backup de '$BANCO'"

# Escreve primeiro num temporário: se o dump falhar no meio, o backup de
# ontem continua íntegro em vez de virar um arquivo pela metade.
TEMP="$ARQUIVO.parcial"
if docker compose exec -T db pg_dump -U "$USUARIO" -d "$BANCO" | gzip > "$TEMP"; then
    mv "$TEMP" "$ARQUIVO"
    echo "[$(date +'%F %T')] ok: $ARQUIVO ($(du -h "$ARQUIVO" | cut -f1))"
else
    rm -f "$TEMP"
    echo "[$(date +'%F %T')] FALHOU — o backup anterior foi preservado" >&2
    exit 1
fi

# Rotação: remove o que passou do limite.
mapfile -t ANTIGOS < <(ls -1t "$DESTINO/${BANCO}-"*.sql.gz 2>/dev/null | tail -n +$((MANTER + 1)))
for velho in "${ANTIGOS[@]:-}"; do
    [ -n "$velho" ] && rm -f "$velho" && echo "  removido: $(basename "$velho")"
done

echo "[$(date +'%F %T')] cópias guardadas: $(ls -1 "$DESTINO/${BANCO}-"*.sql.gz 2>/dev/null | wc -l)"
