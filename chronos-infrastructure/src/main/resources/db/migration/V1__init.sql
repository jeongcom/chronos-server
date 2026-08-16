CREATE SCHEMA IF NOT EXISTS chronos;

CREATE TABLE chronos.space (
    space_id VARCHAR(100) PRIMARY KEY,
    parent_space_id VARCHAR(100),
    name VARCHAR(200) NOT NULL,
    space_type VARCHAR(50) NOT NULL,
    timezone VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_space_parent FOREIGN KEY(parent_space_id) REFERENCES chronos.space(space_id)
);

CREATE TABLE chronos.device (
    device_id VARCHAR(100) PRIMARY KEY,
    space_id VARCHAR(100),
    device_type VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    protocol VARCHAR(50),
    protocol_version VARCHAR(30),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT fk_device_space FOREIGN KEY(space_id) REFERENCES chronos.space(space_id)
);

CREATE TABLE chronos.event_store (
    event_seq BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(150) NOT NULL,
    schema_version INTEGER NOT NULL CHECK(schema_version >= 1),
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(100) NOT NULL,
    source_sequence BIGINT,
    space_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    correlation_id UUID,
    causation_id UUID,
    confidence NUMERIC(5,4) NOT NULL DEFAULT 1.0 CHECK(confidence >= 0 AND confidence <= 1),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_event_occurred ON chronos.event_store(occurred_at);
CREATE INDEX ix_event_space_time ON chronos.event_store(space_id, occurred_at, event_seq);
CREATE INDEX ix_event_source_time ON chronos.event_store(source_id, occurred_at);
CREATE INDEX ix_event_type_time ON chronos.event_store(event_type, occurred_at);
CREATE UNIQUE INDEX ux_event_source_sequence ON chronos.event_store(source_id, source_sequence) WHERE source_sequence IS NOT NULL;

CREATE TABLE chronos.event_outbox (
    outbox_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200),
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);
CREATE INDEX ix_outbox_pending ON chronos.event_outbox(outbox_id) WHERE published_at IS NULL;

CREATE TABLE chronos.world_snapshot (
    snapshot_id UUID PRIMARY KEY,
    space_id VARCHAR(100) NOT NULL,
    snapshot_at TIMESTAMPTZ NOT NULL,
    last_event_seq BIGINT NOT NULL,
    object_count INTEGER NOT NULL DEFAULT 0,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX ix_snapshot_space_time ON chronos.world_snapshot(space_id, snapshot_at DESC);

INSERT INTO chronos.space(space_id,name,space_type,timezone)
VALUES ('LAB-001','CHRONOS Lab 001','ROOM','Asia/Seoul') ON CONFLICT DO NOTHING;
