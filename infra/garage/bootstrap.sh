#!/bin/sh
# Prepares a fresh Garage node for local development.
#
# None of this can live in garage.toml: the cluster layout, the bucket, the access
# key and the CORS rules are cluster state, not configuration. Running it as a
# one-shot service means `compose up` yields a working store instead of a page of
# manual commands.
#
# Uses the admin API rather than the `garage` CLI: the Garage image ships the binary
# on an otherwise empty filesystem with no shell, so a mounted script cannot run there.
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
until api GET /v2/GetClusterStatus >/dev/null 2>&1; do
    sleep 2
done

NODE_ID="$(api GET /v2/GetClusterStatus | jq -r '.nodes[0].id')"

# A node with no layout accepts no writes. One zone, one node, 1G of capacity —
# enough for local attachments and avatars.
ROLE="$(api GET /v2/GetClusterLayout | jq -r --arg id "$NODE_ID" '.roles[] | select(.id == $id) | .id')"
if [ -z "$ROLE" ]; then
    echo "Assigning cluster layout to node $NODE_ID"
    api POST /v2/UpdateClusterLayout \
        -d "{\"roles\":[{\"id\":\"$NODE_ID\",\"zone\":\"local\",\"capacity\":1000000000,\"tags\":[]}]}" >/dev/null
    VERSION="$(api GET /v2/GetClusterLayout | jq -r '.version')"
    api POST /v2/ApplyClusterLayout -d "{\"version\":$((VERSION + 1))}" >/dev/null
fi

BUCKET_ID="$(api GET "/v2/GetBucketInfo?globalAlias=$OBJECT_STORAGE_BUCKET" | jq -r '.id // empty')"
if [ -z "$BUCKET_ID" ]; then
    echo "Creating bucket $OBJECT_STORAGE_BUCKET"
    BUCKET_ID="$(api POST /v2/CreateBucket -d "{\"globalAlias\":\"$OBJECT_STORAGE_BUCKET\"}" | jq -r '.id')"
fi

# Deterministic credentials, so application config does not have to be rewritten
# after every reset. Local only — production issues its own.
#
# Garage validates the shape of an imported key: the id must be `GK` followed by
# 12 hex-encoded bytes, and the secret 32 hex-encoded bytes. An arbitrary string
# such as "veds-local-access-key" is rejected outright.
#
# The lookup lists keys and filters instead of asking the API to search, because
# a search with no match answers with an *error object* rather than an empty
# array — indexing that with `.[0]` aborts jq, and `set -e` then aborts this
# script before the bucket is ever usable.
KEY_ID="$(api GET /v2/ListKeys | jq -r --arg k "$OBJECT_STORAGE_ACCESS_KEY" '.[] | select(.id == $k) | .id' | head -n 1)"
if [ -z "$KEY_ID" ]; then
    echo "Importing local access key"
    KEY_ID="$(api POST /v2/ImportKey \
        -d "{\"accessKeyId\":\"$OBJECT_STORAGE_ACCESS_KEY\",\"secretAccessKey\":\"$OBJECT_STORAGE_SECRET_KEY\",\"name\":\"veds-local\"}" \
        | jq -r '.accessKeyId')"
fi

api POST /v2/AllowBucketKey \
    -d "{\"bucketId\":\"$BUCKET_ID\",\"accessKeyId\":\"$KEY_ID\",\"permissions\":{\"read\":true,\"write\":true,\"owner\":false}}" \
    >/dev/null

# The browser PUTs straight to the store with a pre-signed URL, so its origin has
# to be allowed on the bucket. Without this every upload dies in the preflight and
# the failure looks like an application bug rather than a storage setting.
#
# The rule fields are S3's own PascalCase names (`AllowedOrigin`, not
# `allowedOrigins`); Garage rejects the camelCase spelling.
#
# The bucket itself stays private: allowing an origin is not the same as allowing
# anonymous reads, and no public policy is set anywhere here.
echo "Allowing browser uploads from $FRONTEND_ORIGIN"
api POST "/v2/UpdateBucket?id=$BUCKET_ID" -d "{
  \"corsRules\": [{
    \"AllowedOrigin\": [\"$FRONTEND_ORIGIN\"],
    \"AllowedMethod\": [\"GET\", \"PUT\", \"HEAD\"],
    \"AllowedHeader\": [\"*\"],
    \"ExposeHeader\": [\"ETag\"],
    \"MaxAgeSeconds\": 3600
  }]
}" >/dev/null

echo "Object storage ready: bucket=$OBJECT_STORAGE_BUCKET"
