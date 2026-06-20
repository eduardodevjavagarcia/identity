CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS identity_event (
    stream_id       UUID        NOT NULL,
    sequence_number BIGINT      NOT NULL,
    event_type      TEXT        NOT NULL,
    payload         BYTEA       NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (stream_id, sequence_number, occurred_at)
);

SELECT create_hypertable('identity_event', 'occurred_at', if_not_exists => TRUE);

CREATE TABLE IF NOT EXISTS processed_command (
    command_id   UUID        NOT NULL PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS account_view (
    account_id   UUID        NOT NULL PRIMARY KEY,
    subject_type TEXT        NOT NULL,
    status       TEXT        NOT NULL,
    assurance    TEXT        NOT NULL,
    roles        TEXT        NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS credential_view (
    credential_id UUID        NOT NULL PRIMARY KEY,
    account_id    UUID        NOT NULL,
    status        TEXT        NOT NULL,
    factor_type   TEXT        NOT NULL,
    assurance     TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS compliance_reference_view (
    reference_id           UUID        NOT NULL PRIMARY KEY,
    source_type            TEXT        NOT NULL,
    purpose_reference      TEXT,
    legal_basis_reference  TEXT,
    updated_at             TIMESTAMPTZ NOT NULL
);
