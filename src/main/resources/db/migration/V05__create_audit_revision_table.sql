CREATE TABLE audit_revision
(
    audit_revision_id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    entity_name       TEXT                     NOT NULL,
    entity_id         BIGINT                   NOT NULL,
    operation         TEXT                     NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    entity_state      JSONB                    NOT NULL,
    actor             TEXT                     NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_revision_entity ON audit_revision (entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_revision_operation ON audit_revision (operation);
CREATE INDEX IF NOT EXISTS idx_audit_revision_created_at ON audit_revision (created_at);
