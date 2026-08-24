-- The avatar becomes a reference to a file held by file-service.
--
-- It used to be a path string written by the old NestJS upload endpoint. Files
-- now live in object storage behind file-service, so what a user record holds is
-- an id it can exchange for a signed URL — never a URL, which would expire, and
-- never a path, which would assume this service serves the bytes.
--
-- Dropped and re-added rather than converted: the old values are paths into a
-- storage layout that no longer exists, so there is nothing to migrate.

ALTER TABLE "user" DROP COLUMN IF EXISTS profile_picture;
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS avatar_file_id UUID;
