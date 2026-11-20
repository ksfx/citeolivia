-- PDF attachments of references, stored directly in PostgreSQL (bytea).
-- Separate table, accessed with explicit SQL only, so that lists never load file contents.

CREATE TABLE reference_attachment (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id         BIGINT       NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    reference_entry_id BIGINT       NOT NULL REFERENCES reference_entry (id) ON DELETE CASCADE,
    file_name          VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    size_bytes         BIGINT       NOT NULL,
    sha256             CHAR(64)     NOT NULL,
    data               BYTEA        NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_attachment_per_reference UNIQUE (reference_entry_id, sha256)
);

CREATE INDEX ix_attachment_reference ON reference_attachment (reference_entry_id);
CREATE INDEX ix_attachment_project ON reference_attachment (project_id);
