CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS subscriptions
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                     NOT NULL,
    service_name    VARCHAR(255)             NOT NULL,
    status          VARCHAR(50)              NOT NULL,
    description     TEXT,
    cost            NUMERIC(10,2)            NOT NULL,
    start_at        TIMESTAMPTZ              NOT NULL,
    end_at          TIMESTAMPTZ              NOT NULL,
    created_at      TIMESTAMPTZ              NOT NULL,
    updated_at      TIMESTAMPTZ              NOT NULL
);