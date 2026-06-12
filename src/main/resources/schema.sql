-- One-time cleanup for the Cloudinary → MinIO + Organizations migration.
-- Runs on every startup BEFORE Hibernate ddl-auto. All statements are idempotent.
-- User directive: "xóa hết" — legacy documents are discarded.

-- 1. Wipe all legacy document data so Hibernate can add NOT NULL columns safely.
--    Order matters due to FKs.
DELETE FROM workflow_history   WHERE 1=1;
DELETE FROM document_versions  WHERE 1=1;
DELETE FROM document_tags      WHERE 1=1;
DELETE FROM documents          WHERE 1=1;

-- 2. Drop legacy Cloudinary columns if they still exist.
ALTER TABLE IF EXISTS documents          DROP COLUMN IF EXISTS latest_file_url;
ALTER TABLE IF EXISTS document_versions  DROP COLUMN IF EXISTS file_url;
ALTER TABLE IF EXISTS document_versions  DROP COLUMN IF EXISTS cloudinary_public_id;

-- 3. Drop outdated CHECK constraints on enum columns so Hibernate recreates them
--    with the latest set of enum values (e.g. adding PUBLIC to DocumentVisibility).
ALTER TABLE IF EXISTS documents          DROP CONSTRAINT IF EXISTS documents_visibility_check;
ALTER TABLE IF EXISTS organizations      DROP CONSTRAINT IF EXISTS organizations_visibility_check;
ALTER TABLE IF EXISTS organization_members DROP CONSTRAINT IF EXISTS organization_members_org_role_check;
ALTER TABLE IF EXISTS document_collaborators DROP CONSTRAINT IF EXISTS document_collaborators_permission_check;

-- 4. Drop then recreate FK constraints with ON DELETE CASCADE.
--    Hibernate ddl-auto:update does NOT add CASCADE to existing FKs, so we manage
--    these constraints manually. The DROP searches by column name (constraint name
--    is Hibernate-generated and unpredictable). The ADD uses named constraints so
--    subsequent runs detect duplicates and skip gracefully.
DO $$
DECLARE rec RECORD;
BEGIN
  FOR rec IN
    SELECT c.conname AS constraint_name, t.relname AS table_name
    FROM   pg_constraint c
    JOIN   pg_class      t  ON t.oid  = c.conrelid
    JOIN   pg_class      rt ON rt.oid = c.confrelid
    WHERE  c.contype = 'f'
      AND  rt.relname IN ('organizations', 'documents')
      AND  t.relname  IN ('organization_members', 'documents',
                          'document_versions', 'document_collaborators',
                          'workflow_history', 'notifications')
  LOOP
    EXECUTE format('ALTER TABLE %I DROP CONSTRAINT IF EXISTS %I',
                   rec.table_name, rec.constraint_name);
  END LOOP;
END$$;

DO $$ BEGIN
  ALTER TABLE organization_members
    ADD CONSTRAINT fk_org_member_org
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;

DO $$ BEGIN
  ALTER TABLE documents
    ADD CONSTRAINT fk_doc_org
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;

DO $$ BEGIN
  ALTER TABLE document_versions
    ADD CONSTRAINT fk_ver_doc_cascade
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;

DO $$ BEGIN
  ALTER TABLE document_collaborators
    ADD CONSTRAINT fk_collab_doc_cascade
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;

DO $$ BEGIN
  ALTER TABLE workflow_history
    ADD CONSTRAINT fk_wf_doc_cascade
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object THEN NULL;
END$$;
