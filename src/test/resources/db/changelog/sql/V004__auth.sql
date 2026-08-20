--liquibase formatted sql

--changeset arc-dev:V004-otp-token
create table otp_token (
  id         uuid primary key default gen_random_uuid(),
  email      varchar not null,
  code       varchar(6) not null,
  expires_at timestamptz not null,
  used_at    timestamptz,
  created_at timestamptz not null default now()
);

--changeset arc-dev:V004-otp-token-index
create index otp_token_email_idx on otp_token (email, expires_at);

--changeset arc-dev:V004-customer-session
create table customer_session (
  id          uuid primary key default gen_random_uuid(),
  customer_id uuid not null references customer (id),
  token       text not null unique,
  expires_at  timestamptz not null,
  created_at  timestamptz not null default now()
);

--changeset arc-dev:V004-customer-session-index
create index customer_session_token_idx on customer_session (token);

--changeset arc-dev:V004-staff-session
create table staff_session (
  id         uuid primary key default gen_random_uuid(),
  staff_id   uuid not null references staff (id),
  token      text not null unique,
  expires_at timestamptz not null,
  created_at timestamptz not null default now()
);

--changeset arc-dev:V004-staff-session-index
create index staff_session_token_idx on staff_session (token);
