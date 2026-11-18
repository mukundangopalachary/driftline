-- Conventions: docs/decisions/0013-schema-conventions.md
-- Auth design: docs/decisions/0006, 0009

create table users (
    id           uuid        not null default gen_random_uuid(),
    github_id    bigint      not null,
    github_login text        not null,
    name         text,
    avatar_url   text,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    constraint pk_users primary key (id),
    constraint uq_users_github_id unique (github_id)
);

-- Only a hash of the refresh token is stored, never the token itself (0009).
create table refresh_tokens (
    id         uuid        not null default gen_random_uuid(),
    user_id    uuid        not null,
    token_hash text        not null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    constraint pk_refresh_tokens primary key (id),
    constraint uq_refresh_tokens_token_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users (id) on delete cascade
);

create index ix_refresh_tokens_user_id on refresh_tokens (user_id);
