# Garage (object storage)

S3-compatible storage for `file-service`. The browser uploads and downloads here directly with
pre-signed URLs, so file bytes never pass through a JVM.

|             |                           |
|-------------|---------------------------|
| S3 API      | `localhost:9100`          |
| Web console | `localhost:9101`          |
| Admin API   | `localhost:9102`          |
| Bucket      | `veds-files`, **private** |

## Why a bootstrap script

A fresh Garage node has no cluster layout, no bucket and no access keys, and none of that can be
expressed in `garage.toml` — it is cluster state applied through the CLI. `bootstrap.sh` runs as
a one-shot compose service so that `docker compose up` yields a working store instead of a page
of manual commands in a README nobody reads.

Every step is idempotent, so a restart is harmless.

## CORS is required

The browser `PUT`s to the store from the SPA's origin, so the bucket must allow it. Without this
every upload dies in the preflight and the failure looks like an application bug rather than a
storage setting.

Allowing an origin is **not** the same as allowing anonymous reads. The bucket stays private:
nothing is reachable without a signed URL.

## Local only

`garage.toml` here is a single node with `replication_factor = 1` and a fixed `rpc_secret` — a
laptop, not a cluster. A real deployment supplies its own configuration and generates the secret
per node (`openssl rand -hex 32`).

## One compatibility caveat

`file-service` sets `response-content-disposition` on download URLs so the browser saves the
file under its original name. That is an S3 feature a compatible store may not implement. If it
is ignored the download still works — the file is named after the object key — so this degrades
rather than fails.

See [Files](../../docs/files.md).
