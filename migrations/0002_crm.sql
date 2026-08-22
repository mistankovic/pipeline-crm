create table if not exists crm_users (
  id text primary key,
  email text not null,
  name text not null,
  role text not null
);

create table if not exists companies (
  id text primary key,
  name text not null,
  domain text,
  notes text
);

create table if not exists contacts (
  id text primary key,
  company_id text not null references companies(id),
  name text not null,
  email text,
  title text
);

create table if not exists deals (
  id text primary key,
  title text not null,
  company_id text not null references companies(id),
  owner_id text not null references crm_users(id),
  value_minor integer not null,
  currency text not null,
  probability integer not null,
  stage text not null
);

create table if not exists activities (
  id text primary key,
  type text not null,
  body text not null,
  created_by_user_id text not null references crm_users(id),
  deal_id text references deals(id),
  contact_id text references contacts(id),
  occurred_at timestamptz not null
);

create index if not exists deals_stage_idx on deals (stage);
create index if not exists deals_owner_idx on deals (owner_id);
create index if not exists activities_deal_idx on activities (deal_id);
create index if not exists activities_contact_idx on activities (contact_id);
create index if not exists contacts_company_idx on contacts (company_id);
