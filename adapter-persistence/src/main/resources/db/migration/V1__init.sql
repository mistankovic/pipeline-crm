CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL
);

CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL
);

CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies (id),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320)
);

CREATE TABLE deals (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies (id),
    owner_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(180) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    probability SMALLINT NOT NULL,
    stage VARCHAR(32) NOT NULL
);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    deal_id UUID REFERENCES deals (id),
    contact_id UUID REFERENCES contacts (id),
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT activities_one_target CHECK (
        (deal_id IS NULL) <> (contact_id IS NULL)
    )
);

CREATE INDEX activities_deal_id_idx ON activities (deal_id);
CREATE INDEX activities_contact_id_idx ON activities (contact_id);
CREATE INDEX deals_owner_id_idx ON deals (owner_id);
CREATE INDEX deals_stage_idx ON deals (stage);
