ALTER TABLE chronos.device
    ADD COLUMN IF NOT EXISTS credential_salt VARCHAR(128),
    ADD COLUMN IF NOT EXISTS credential_hash VARCHAR(256),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
    ADD COLUMN IF NOT EXISTS last_gateway_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS last_connection_id VARCHAR(160),
    ADD COLUMN IF NOT EXISTS last_sequence BIGINT,
    ADD COLUMN IF NOT EXISTS authenticated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS offline_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS ix_device_status ON chronos.device(status);
CREATE INDEX IF NOT EXISTS ix_device_last_seen ON chronos.device(last_seen_at);
