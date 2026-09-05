#!/bin/bash
# =============================================================================
# veds-provisioner entrypoint
# =============================================================================
set -euo pipefail

: "${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS is required}"
: "${SCHEMA_REGISTRY_URL:?SCHEMA_REGISTRY_URL is required}"
SCHEMA_COMPATIBILITY="${SCHEMA_COMPATIBILITY:-BACKWARD}"

echo "=========================================="
echo "VEDS provisioner"
echo "  KAFKA_BOOTSTRAP_SERVERS = ${KAFKA_BOOTSTRAP_SERVERS}"
echo "  SCHEMA_REGISTRY_URL     = ${SCHEMA_REGISTRY_URL}"
echo "  SCHEMA_COMPATIBILITY    = ${SCHEMA_COMPATIBILITY}"
echo "=========================================="

# ----------------------------------------------------------------------------
# Step 1: Terraform - Kafka topics
# ----------------------------------------------------------------------------
echo
echo "==> [1/2] Provisioning Kafka topics via Terraform"

# Convert "host:9092,host2:9092" -> ["host:9092","host2:9092"] for list(string).
TF_VAR_bootstrap_servers="$(python3 -c "import json,os; print(json.dumps([s.strip() for s in os.environ['KAFKA_BOOTSTRAP_SERVERS'].split(',') if s.strip()]))")"
export TF_VAR_bootstrap_servers

# Broker TLS (SSL listener). KAFKA_TLS_ENABLED=true + KAFKA_CA_CERT_FILE point
# the provider at the cluster's internal CA (see infra/kafka/main.tf).
export TF_VAR_tls_enabled="${KAFKA_TLS_ENABLED:-false}"
export TF_VAR_ca_cert_file="${KAFKA_CA_CERT_FILE:-}"

cd /workspace/tf
terraform init -input=false

# Adopt topics that already exist on the broker but are missing from the
# state (e.g. state lost or created before the /state backend existed).
# `terraform import` on a missing topic just fails -> `|| true` lets apply
# create it; import on an already-managed address is skipped by state check.
import_if_missing() {
    local addr="$1" topic="$2"
    if ! terraform state show "$addr" >/dev/null 2>&1; then
        echo "  importing pre-existing topic ${topic} into state (${addr})"
        terraform import -input=false "$addr" "$topic" >/dev/null 2>&1 || true
    fi
}
for topic in mail-requested mail-sent mail-failed saga-compensation-mail; do
    import_if_missing "kafka_topic.business[\"${topic}\"]" "${topic}"
    import_if_missing "kafka_topic.dlt[\"${topic}\"]" "${topic}-dlt"
done

terraform apply -auto-approve -input=false

# ----------------------------------------------------------------------------
# Step 2: Schema Registry - Avro contracts
# ----------------------------------------------------------------------------
echo
echo "==> [2/2] Registering Avro schemas in Schema Registry"

cd /workspace
python3 register_schemas.py \
    --schemas-dir /workspace/contracts \
    --registry-url "${SCHEMA_REGISTRY_URL}" \
    --compatibility "${SCHEMA_COMPATIBILITY}"

echo
echo "=========================================="
echo "OK - Provisioning complete"
echo "=========================================="
