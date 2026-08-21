--liquibase formatted sql

--changeset ark-dev:V002-segment
create table segment (
  id                     uuid primary key default gen_random_uuid(),
  axis                   varchar not null check (axis in ('entity', 'service', 'sector')),
  code                   varchar not null,
  label                  varchar not null,
  classification_trigger jsonb,
  question_overrides     jsonb,
  doc_overrides          jsonb,
  created_at             timestamptz not null default now(),
  deactivated_at         timestamptz
);

--changeset ark-dev:V002-segment-index
create unique index segment_code_active_uq on segment (code) where deactivated_at is null;

--changeset ark-dev:V002-staff
create table staff (
  id         uuid primary key default gen_random_uuid(),
  email      varchar not null unique,
  name       varchar not null,
  role       varchar not null check (role in ('SALES', 'OPS', 'COMPLIANCE', 'ADMIN')),
  is_active  boolean not null default true,
  created_at timestamptz not null default now()
);

--changeset ark-dev:V002-question
create table question (
  id                    uuid primary key default gen_random_uuid(),
  code                  varchar not null,
  phase                 varchar not null check (phase in ('first', 'second')),
  classification        varchar not null check (classification in ('common', 'own')),
  owner_segment_id      uuid references segment (id),
  label                 text not null,
  input_type            varchar not null check (input_type in ('text', 'textarea', 'select', 'radio', 'multi', 'number', 'date')),
  options               jsonb,
  is_required           boolean not null default false,
  show_when             jsonb,
  repeat                boolean not null default false,
  parent_question_id    uuid references question (id),
  display_order         int not null default 0,
  replaces_question_id  uuid references question (id),
  created_by_staff_id   uuid references staff (id),
  created_at            timestamptz not null default now(),
  deactivated_at        timestamptz
);

--changeset ark-dev:V002-question-index
create unique index question_code_active_uq on question (code) where deactivated_at is null;

--changeset ark-dev:V002-question-immutable-trigger splitStatements:false
create or replace function question_enforce_immutable() returns trigger as $$
begin
  if (to_jsonb(new) - 'deactivated_at') is distinct from (to_jsonb(old) - 'deactivated_at') then
    raise exception 'question rows are immutable: only deactivated_at may change. To edit, deactivate and insert a new row (set replaces_question_id).';
  end if;
  return new;
end;
$$ language plpgsql;
create trigger question_immutable
  before update on question
  for each row execute function question_enforce_immutable();

--changeset ark-dev:V002-doc-template
create table doc_template (
  id               uuid primary key default gen_random_uuid(),
  type             varchar not null,
  display_name     varchar not null,
  classification   varchar not null check (classification in ('common', 'own')),
  owner_segment_id uuid references segment (id),
  is_required      boolean not null default true,
  is_conditional   boolean not null default false,
  condition        jsonb,
  guide            text,
  created_at       timestamptz not null default now(),
  deactivated_at   timestamptz
);

--changeset ark-dev:V002-doc-template-index
create unique index doc_template_type_active_uq on doc_template (type) where deactivated_at is null;

--changeset ark-dev:V002-customer
create table customer (
  id              uuid primary key default gen_random_uuid(),
  email           varchar not null unique,
  auth_method     varchar not null default 'otp' check (auth_method in ('otp', 'password')),
  password_hash   varchar,
  business_reg_no varchar,
  company_name    varchar,
  contact_name    varchar,
  created_at      timestamptz not null default now()
);

--changeset ark-dev:V002-customer-index
create index customer_biz_reg_no_idx on customer (business_reg_no);

--changeset ark-dev:V002-onboarding-case
create table onboarding_case (
  id                      uuid primary key default gen_random_uuid(),
  customer_id             uuid not null references customer (id),
  status                  varchar not null default 'INQUIRY_RECEIVED' check (status in (
                            'INQUIRY_RECEIVED', 'DOCUMENT_SUBMISSION_REQUIRED',
                            'INITIAL_SCREENING', 'DOCUMENT_SCREENING_REQUIRED',
                            'APPROVAL_REVIEW_REQUIRED', 'ACCOUNT_SETUP_REQUIRED',
                            'REVISION_REQUESTED', 'COMPLETED', 'CLOSED')),
  close_reason            varchar check (close_reason in ('DROPPED', 'EXITED')),
  revision_requested_from varchar,
  entity_code             varchar,
  services                text[] not null default '{}',
  sectors                 text[] not null default '{}',
  segment_meta            jsonb not null default '{}',
  pinned_question_ids     jsonb not null default '{}',
  assignee_staff_id       uuid references staff (id),
  last_customer_action_at timestamptz,
  created_at              timestamptz not null default now(),
  updated_at              timestamptz not null default now()
);

--changeset ark-dev:V002-case-indexes
create unique index case_one_active_per_customer_uq on onboarding_case (customer_id)
  where status not in ('COMPLETED', 'CLOSED');
create index case_dashboard_idx on onboarding_case (status, updated_at desc);

--changeset ark-dev:V002-intake-response
create table intake_response (
  id           uuid primary key default gen_random_uuid(),
  case_id      uuid not null references onboarding_case (id),
  phase        varchar not null check (phase in ('first', 'second')),
  status       varchar not null default 'not_started' check (status in ('not_started', 'submitted')),
  answers      jsonb not null default '{}',
  saved_at     timestamptz not null default now(),
  submitted_at timestamptz,
  unique (case_id, phase)
);

--changeset ark-dev:V002-document
create table document (
  id              uuid primary key default gen_random_uuid(),
  case_id         uuid not null references onboarding_case (id),
  doc_template_id uuid not null references doc_template (id),
  type            varchar not null,
  display_name    varchar not null,
  status          varchar not null default 'REQUESTED' check (status in (
                    'NOT_REQUESTED', 'REQUESTED', 'SUBMITTED', 'REVISION_REQUIRED', 'APPROVED')),
  is_required     boolean not null default true,
  is_conditional  boolean not null default false,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  unique (case_id, type)
);

--changeset ark-dev:V002-document-file
create table document_file (
  id                uuid primary key default gen_random_uuid(),
  document_id       uuid not null references document (id),
  file_name         varchar not null,
  file_size         int not null,
  mime_type         varchar not null,
  storage_key       varchar not null,
  uploader_type     varchar not null check (uploader_type in ('CUSTOMER', 'STAFF')),
  uploader_staff_id uuid references staff (id),
  is_latest         boolean not null default true,
  uploaded_at       timestamptz not null default now(),
  check ((uploader_type = 'STAFF') = (uploader_staff_id is not null))
);

--changeset ark-dev:V002-document-file-index
create index document_file_document_idx on document_file (document_id);

--changeset ark-dev:V002-revision-request
create table revision_request (
  id                     uuid primary key default gen_random_uuid(),
  document_id            uuid not null references document (id),
  reason                 text not null,
  requested_by_staff_id  uuid not null references staff (id),
  requested_from_status  varchar not null,
  requested_at           timestamptz not null default now(),
  resolved_at            timestamptz
);

--changeset ark-dev:V002-revision-request-index
create index revision_request_open_idx on revision_request (document_id) where resolved_at is null;

--changeset ark-dev:V002-case-event
create table case_event (
  id         uuid primary key default gen_random_uuid(),
  case_id    uuid not null references onboarding_case (id),
  event_type varchar not null check (event_type in (
               'CASE_CREATED', 'CASE_STATUS_CHANGED', 'DOC_STATUS_CHANGED', 'ASSIGNEE_CHANGED')),
  actor_type varchar not null check (actor_type in ('CUSTOMER', 'STAFF', 'SYSTEM')),
  actor_id   uuid,
  payload    jsonb not null default '{}',
  created_at timestamptz not null default now()
);

--changeset ark-dev:V002-case-event-index
create index case_event_timeline_idx on case_event (case_id, created_at);
