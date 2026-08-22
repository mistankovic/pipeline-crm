-- PipelineCRM initial schema.
--
-- The migrations live in the persistence adapter because the schema is that adapter's own
-- business. Flyway finds them on the classpath, so the application module that starts the
-- database never has to know where they came from.
--
-- The database enforces what a database can enforce: identity, referential integrity, ranges,
-- and the "exactly one subject" rule for activities. Business rules stay in the domain --
-- these constraints are a second line of defence against corrupt data, never the place a rule
-- is decided.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    name          VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    CONSTRAINT users_role_is_known CHECK (role IN ('SALES', 'MANAGER'))
);

CREATE TABLE companies (
    id   UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE contacts (
    id         UUID PRIMARY KEY,
    company_id UUID         NOT NULL REFERENCES companies (id),
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(320) NOT NULL
);

CREATE INDEX contacts_by_company ON contacts (company_id);

CREATE TABLE deals (
    id             UUID PRIMARY KEY,
    title          VARCHAR(200)   NOT NULL,
    company_id     UUID           NOT NULL REFERENCES companies (id),
    owner_id       UUID           NOT NULL REFERENCES users (id),
    value_amount   NUMERIC(19, 4) NOT NULL,
    value_currency VARCHAR(3)     NOT NULL,
    probability    INTEGER        NOT NULL,
    stage          VARCHAR(20)    NOT NULL,
    CONSTRAINT deals_value_is_not_negative CHECK (value_amount >= 0),
    CONSTRAINT deals_probability_is_a_percentage CHECK (probability BETWEEN 0 AND 100),
    CONSTRAINT deals_stage_is_known CHECK (
        stage IN ('LEAD', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'CLOSED_WON', 'CLOSED_LOST'))
);

CREATE INDEX deals_by_owner ON deals (owner_id);

CREATE TABLE activities (
    id          UUID PRIMARY KEY,
    deal_id     UUID REFERENCES deals (id),
    contact_id  UUID REFERENCES contacts (id),
    type        VARCHAR(20)   NOT NULL,
    summary     VARCHAR(2000) NOT NULL,
    author_id   UUID          NOT NULL REFERENCES users (id),
    occurred_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT activities_type_is_known CHECK (type IN ('NOTE', 'CALL', 'MEETING')),
    -- The domain makes "linked to a deal or a contact, never both, never neither"
    -- unrepresentable with a sealed type. The database says the same thing in its own words,
    -- so no other writer can put a row here that the domain could not have produced.
    CONSTRAINT activities_have_exactly_one_subject CHECK (
        (deal_id IS NOT NULL AND contact_id IS NULL)
        OR (deal_id IS NULL AND contact_id IS NOT NULL))
);

CREATE INDEX activities_by_deal ON activities (deal_id);
CREATE INDEX activities_by_contact ON activities (contact_id);
