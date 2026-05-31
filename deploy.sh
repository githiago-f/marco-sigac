
#!/usr/bin/env bash

set -euo pipefail

###########################################################
# CONFIGURAÇÕES
###########################################################

APP_NAME="marco-sigac"

APP_DIR="/opt/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"

DOMAIN="sigac.seudominio.com"
EMAIL="admin@seudominio.com"
DOCKER_IMAGE="ghcr.io/githiago-f/marco-sigac:latest"

CPU_LIMIT="2"
MEMORY_LIMIT="3G"
POSTGRES_MEMORY="3G"
POSTGRES_CPU="2"

POSTGRES_DB="marco_sigac"
POSTGRES_USER="marco"
POSTGRES_PASSWORD="<senha>"

###########################################################
# DEPENDÊNCIAS
###########################################################

apt update

apt install -y \
  docker.io \
  docker-compose-plugin \
  curl \
  jq

systemctl enable docker
systemctl start docker

###########################################################
# DIRETÓRIOS
###########################################################

mkdir -p "${APP_DIR}"
mkdir -p "${LOG_DIR}"
mkdir -p "${APP_DIR}/caddy"
mkdir -p "${APP_DIR}/postgres"
mkdir -p "${APP_DIR}/uploads"

chmod 755 "${APP_DIR}/uploads"

###########################################################
# CADDYFILE
###########################################################

cat > "${APP_DIR}/caddy/Caddyfile" <<EOF
${DOMAIN} {

    encode gzip zstd

    reverse_proxy marco-sigac:8080

    tls ${EMAIL}

    log {
        output file ${LOG_DIR}/caddy.log
    }
}
EOF

###########################################################
# .ENV
###########################################################

cat > "${APP_DIR}/.env" <<EOF
DB_PROVIDER=postgresql
DB_USER=${POSTGRES_USER}
DB_PASSWORD=${POSTGRES_PASSWORD}
DB_CONNECTION_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}
DB_POOLSIZE=10

QUARKUS_HTTP_PORT=8080

EOF

###########################################################
# COMPOSE
###########################################################

cat > "${APP_DIR}/compose.yml" <<EOF
services:

  marco-sigac:
    image: ${DOCKER_IMAGE}
    container_name: marco-sigac
    restart: unless-stopped

    env_file:
      - .env

    depends_on:
      - postgres

    expose:
      - "8080"

    mem_limit: ${MEMORY_LIMIT}
    cpus: ${CPU_LIMIT}

    networks:
      - internal

    volumes:
      - /opt/marco-sigac/uploads:/work/uploads

    logging:
      driver: json-file

      options:
        max-size: "10m"

        max-file: "3"

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/"]
      interval: 30s
      timeout: 5s
      retries: 5
      start_period: 60s

  postgres:
    image: postgres:17
    container_name: marco-postgres
    restart: unless-stopped

    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}

    volumes:
      - ${APP_DIR}/postgres:/var/lib/postgresql/data

    mem_limit: ${POSTGRES_MEMORY}
    cpus: ${POSTGRES_CPU}

    networks:
      - internal

    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  caddy:
    image: caddy:latest
    container_name: marco-caddy
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"

    volumes:
      - ${APP_DIR}/caddy/Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
      - caddy_config:/config

    depends_on:
      - marco-sigac

    networks:
      - internal

    mem_limit: 64m

networks:
  internal:

volumes:
  caddy_data:
  caddy_config:
EOF

###########################################################
# UPDATE.SH
###########################################################

cat > "${APP_DIR}/update.sh" <<'EOF'
#!/usr/bin/env bash

set -euo pipefail

APP_NAME="marco-sigac"

APP_DIR="/opt/${APP_NAME}"

LOG_ROOT="/var/log/${APP_NAME}"

NOW=$(date +"%Y/%m/%d")

HOUR=$(date +"%H-%M-%S")

LOG_PATH="${LOG_ROOT}/${NOW}"

mkdir -p "${LOG_PATH}"

LOG_FILE="${LOG_PATH}/${HOUR}.log"

exec >> "${LOG_FILE}" 2>&1

echo "================================================"
echo "[INFO] $(date)"
echo "================================================"

cd "${APP_DIR}"

OLD_IMAGE=$(docker inspect marco-sigac --format '{{.Image}}' || true)

docker compose pull marco-sigac

NEW_IMAGE=$(docker image inspect ghcr.io/githiago-f/marco-sigac:latest --format '{{.Id}}')

if [[ "${OLD_IMAGE}" == "${NEW_IMAGE}" ]]; then
    echo "[INFO] Sem nova imagem."
    exit 0
fi

echo "[INFO] Nova imagem detectada."

echo "[INFO] Atualizando container..."

docker compose up -d --no-deps marco-sigac

echo "[INFO] Aguardando healthcheck..."

for i in {1..30}; do

    STATUS=$(docker inspect \
      --format='{{json .State.Health.Status}}' \
      marco-sigac || true)

    if [[ "${STATUS}" == "\"healthy\"" ]]; then
        echo "[INFO] Aplicação saudável."
        break
    fi

    echo "[INFO] Tentativa ${i}/30"

    sleep 2
done

echo "[INFO] Limpando imagens antigas..."

docker image prune -af

echo "[INFO] Finalizado."
EOF

chmod +x "${APP_DIR}/update.sh"

###########################################################
# SYSTEMD
###########################################################

cat > "/etc/systemd/system/marco-sigac.service" <<EOF
[Unit]
Description=Marco SIGAC Stack
Requires=docker.service
After=docker.service

[Service]
Type=oneshot

RemainAfterExit=yes

WorkingDirectory=${APP_DIR}

ExecStart=/usr/bin/docker compose up -d

ExecStop=/usr/bin/docker compose down

TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

###########################################################
# TIMER
###########################################################

cat > "/etc/systemd/system/marco-sigac-updater.service" <<EOF
[Unit]
Description=Marco SIGAC Updater

[Service]
Type=oneshot

ExecStart=${APP_DIR}/update.sh
EOF

cat > "/etc/systemd/system/marco-sigac-updater.timer" <<EOF
[Unit]
Description=Run updater every 5 minutes

[Timer]
OnBootSec=2min

OnUnitActiveSec=5min

Unit=marco-sigac-updater.service

[Install]
WantedBy=timers.target
EOF

###########################################################
# LOGROTATE
###########################################################

cat > "/etc/logrotate.d/marco-sigac" <<EOF
${LOG_DIR}/*.log
${LOG_DIR}/*/*/*/*.log {

    daily

    rotate 14

    compress

    delaycompress

    missingok

    notifempty

    copytruncate
}
EOF

###########################################################
# START
###########################################################

systemctl daemon-reload

systemctl enable marco-sigac.service
systemctl start marco-sigac.service

systemctl enable marco-sigac-updater.timer
systemctl start marco-sigac-updater.timer

###########################################################
# FINAL
###########################################################

echo
echo "================================================"
echo "INSTALAÇÃO CONCLUÍDA"
echo "================================================"
echo
echo "Aplicação:"
echo "  ${APP_DIR}"
echo
echo "Logs:"
echo "  ${LOG_DIR}"
echo
echo "Postgres shell:"
echo "  docker exec -it marco-postgres psql -U ${POSTGRES_USER}"
echo
echo "Compose:"
echo "  cd ${APP_DIR}"
echo "  docker compose ps"
echo
echo "Deploy manual:"
echo "  ${APP_DIR}/update.sh"
echo
echo "Caddy:"
echo "  https://${DOMAIN}"
echo

