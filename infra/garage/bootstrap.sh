#!/bin/sh
# Prepares a fresh Garage node for local development.
#
# None of this can live in garage.toml: the cluster layout, the bucket, the access
# key and the CORS rules are cluster state. Running it as a one-shot service means
# `compose up` yields a working store instead of a page of manual commands.
#
# Uses the admin API rather than the `garage` CLI, because the Garage image ships
# the binary on an empty filesystem with no shell — a mounted script cannot run
# there, which is what "crun: executable file /bin/sh not found" was reporting.
#
# Every step is idempotent, so a restart is harmless.
set -eu

ADMIN="${GARAGE_ADMIN_URL:-http://object-storage:3903}"
AUTH="Authorization: Bearer ${GARAGE_ADMIN_TOKEN}"

api() {
    method="$1"
    path="$2"
    shift 2
    curl -sS -X "$method" -H "$AUTH" -H "Content-Type: application/json" "$ADMIN$path" "$@"
}

echo "Waiting for the Garage admin API..."
until api GET /v1/status >/dev/null 2>&1; do
    sleep 2
done

NODE_ID="$(api GET /v1/status | jq -r '.node')"

# A node with no layout accepts no writes. One zone, one node, 1G of capacity —
# enough for local attachments and avatars.
STAGED="$(api GET /v1/layout | jq -r --arg id "$NODE_ID" '.roles[] | select(.id == $id) | .id' || true)"
if [ -z "$STAGED" ]; then
    echo "Assigning cluster layout to node $NODE_ID"
    api POST /v1/layout -d "[{\"id\":\"$NODE_ID\",\"zone\":\"local\",\"capacity\":1000000000,\"tags\":[]}]" >/dev/null
    VERSION="$(api GET /v1/layout | jq -r '.version')"
    api POST /v1/layout/apply -d "{\"version\":$((VERSION + 1))}" >/dev/null
fi

BUCKET_ID="$(api GET "/v1/bucket?globalAlias=$OBJECT_STORAGE_BUCKET" | jq -r '.id // empty')"
if [ -z "$BUCKET_ID" ]; then
    echo "Creating bucket $OBJECT_STORAGE_BUCKET"
    BUCKET_ID="$(api POST /v1/bucket -d "{\"globalAlias\":\"$OBJECT_STORAGE_BUCKET\"}" | jq -r '.id')"
fi

# Deterministic credentials, so application config does not have to be rewritten
# after every reset. Local only — production issues its own.
KEY_ID="$(api GET "/v1/key?search=$OBJECT_STORAGE_ACCESS_KEY" | jq -r '.[0].accessKeyId // empty')"
if [ -z "$KEY_ID" ]; then
    echo "Importing local access key"
    KEY_ID="$(api POST /v1/key/import \
        -d "{\"accessKeyId\":\"$OBJECT_STORAGE_ACCESS_KEY\",\"secretAccessKey\":\"$OBJECT_STORAGE_SECRET_KEY\",\"name\":\"veds-local\"}" \
        | jq -r '.accessKeyId')"
fi

api POST /v1/bucket/allow \
    -d "{\"bucketId\":\"$BUCKET_ID\",\"accessKeyId\":\"$KEY_ID\",\"permissions\":{\"read\":true,\"write\":true,\"owner\":false}}" \
    >/dev/null

# The browser PUTs straight to the store with a pre-signed URL, so its origin has
# to be allowed on the bucket. Without this every upload dies in the preflight and
# the failure looks like an application bug rather than a storage setting.
#
# The bucket itself stays private: allowing an origin is not the same as allowing
# anonymous reads, and no public policy is set anywhere here.
echo "Allowing browser uploads from $FRONTEND_ORIGIN"
api PUT /v1/bucket -d "{
  \"id\": \"$BUCKET_ID\",
  \"corsRules\": [{
    \"allowedOrigins\": [\"$FRONTEND_ORIGIN\"],
    \"allowedMethods\": [\"GET\", \"PUT\", \"HEAD\"],
    \"allowedHeaders\": [\"*\"],
    \"exposeHeaders\": [\"ETag\"],
    \"maxAgeSeconds\": 3600
  }]
}" >/dev/null

echo "Object storage ready: bucket=$OBJECT_STORAGE_BUCKET"
