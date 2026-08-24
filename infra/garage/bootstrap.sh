#!/bin/sh
# Prepares a fresh Garage node for local development.
#
# None of this can live in garage.toml: the layout, the bucket, the access key
# and the CORS rules are cluster state applied through the CLI. Running it as a
# one-shot service means `docker compose up` yields a working store instead of a
# page of manual commands in a README nobody reads.
#
# Every step is idempotent, so a restart is harmless.
set -eu

export GARAGE_RPC_HOST="${GARAGE_RPC_HOST:-object-storage}:3901"

echo "Waiting for the Garage node to answer..."
until /garage status >/dev/null 2>&1; do
    sleep 2
done

NODE_ID="$(/garage node id -q | cut -d@ -f1)"

# A node with no layout accepts no writes. One zone, one node, 1G of capacity —
# enough for local attachments and avatars.
if ! /garage layout show | grep -q "$NODE_ID"; then
    echo "Assigning cluster layout to node $NODE_ID"
    /garage layout assign -z local -c 1G "$NODE_ID"
    /garage layout apply --version 1
fi

if ! /garage bucket list | grep -q "$OBJECT_STORAGE_BUCKET"; then
    echo "Creating bucket $OBJECT_STORAGE_BUCKET"
    /garage bucket create "$OBJECT_STORAGE_BUCKET"
fi

# Deterministic credentials, so application config does not have to be rewritten
# after every reset. Local only — production issues its own.
if ! /garage key list | grep -q "$OBJECT_STORAGE_ACCESS_KEY"; then
    echo "Importing local access key"
    /garage key import \
        --yes \
        -n veds-local \
        "$OBJECT_STORAGE_ACCESS_KEY" \
        "$OBJECT_STORAGE_SECRET_KEY"
fi

/garage bucket allow \
    --read --write \
    "$OBJECT_STORAGE_BUCKET" \
    --key "$OBJECT_STORAGE_ACCESS_KEY"

# The browser PUTs straight to the store with a pre-signed URL, so its origin has
# to be allowed on the bucket. Without this every upload dies in the preflight,
# and the failure looks like a bug in the application rather than in the store.
#
# The bucket itself stays private: allowing an origin is not the same as allowing
# anonymous reads, and no `bucket website` or public policy is set anywhere here.
echo "Allowing browser uploads from $FRONTEND_ORIGIN"
/garage bucket cors-set "$OBJECT_STORAGE_BUCKET" \
    --allow-origin "$FRONTEND_ORIGIN" \
    --allow-method GET --allow-method PUT --allow-method HEAD \
    --allow-header "*" \
    --expose-header ETag \
    --max-age 3600 || \
    echo "cors-set unsupported by this Garage version - set CORS manually, see docs/files.md"

echo "Object storage ready: bucket=$OBJECT_STORAGE_BUCKET"
