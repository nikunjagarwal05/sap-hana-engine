-- Flyway V1 Initial Schema Migration for SAP HANA Engine Metadata & Landing Zone

CREATE TABLE IF NOT EXISTS data_source (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    connector_type VARCHAR(50) NOT NULL, -- 'JDBC' | 'REST'
    config_json JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    poll_interval_seconds INT NOT NULL DEFAULT 300,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discovered_entity (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES data_source(id) ON DELETE CASCADE,
    schema_name VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    discovered_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT idx_entity_source_name UNIQUE (source_id, schema_name, entity_name)
);

CREATE TABLE IF NOT EXISTS discovered_column (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL REFERENCES discovered_entity(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    inferred_type VARCHAR(50) NOT NULL,
    nullable BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS ingestion_job (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES data_source(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL, -- 'RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL'
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    total_entities INT DEFAULT 0,
    total_records BIGINT DEFAULT 0,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS ingestion_run (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES ingestion_job(id) ON DELETE CASCADE,
    entity_id BIGINT NOT NULL REFERENCES discovered_entity(id) ON DELETE CASCADE,
    entity_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    records_fetched BIGINT DEFAULT 0,
    duration_ms BIGINT DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS raw_record (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL REFERENCES discovered_entity(id) ON DELETE CASCADE,
    run_id BIGINT REFERENCES ingestion_run(id) ON DELETE SET NULL,
    fields JSONB NOT NULL,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_raw_record_entity ON raw_record(entity_id);
CREATE INDEX IF NOT EXISTS idx_raw_record_fields_gin ON raw_record USING GIN(fields);

CREATE TABLE IF NOT EXISTS table_profile (
    id BIGSERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL REFERENCES discovered_entity(id) ON DELETE CASCADE,
    row_count BIGINT NOT NULL,
    last_ingested_at TIMESTAMP WITH TIME ZONE,
    profiled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS column_profile (
    id BIGSERIAL PRIMARY KEY,
    table_profile_id BIGINT NOT NULL REFERENCES table_profile(id) ON DELETE CASCADE,
    column_name VARCHAR(255) NOT NULL,
    null_percentage DOUBLE PRECISION NOT NULL,
    distinct_count BIGINT NOT NULL,
    inferred_semantic_type VARCHAR(50) NOT NULL, -- 'NUMERIC', 'CATEGORICAL', 'TEMPORAL', 'TEXT', 'BOOLEAN'
    min_val VARCHAR(255),
    max_val VARCHAR(255),
    mean_val DOUBLE PRECISION,
    stddev_val DOUBLE PRECISION,
    outlier_count INT DEFAULT 0,
    top_frequencies_json JSONB,
    time_buckets_json JSONB
);

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Default Admin User (username: admin, password: adminpassword)
INSERT INTO app_user (username, password_hash, role)
VALUES ('admin', 'DEFAULT_ADMIN_PASSWORD_HASH_PLACEHOLDER', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
