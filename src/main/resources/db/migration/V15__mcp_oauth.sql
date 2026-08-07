-- Claude/ChatGPT UI가 Personal Wiki MCP에 연결할 때 사용하는 OAuth 2.1 상태.
-- 서비스 로그인 JWT와 외부 OAuth token은 분리하며, code/token 원문은 저장하지 않는다.

CREATE TABLE service.oauth_clients (
    client_id                    VARCHAR(160) PRIMARY KEY,
    client_name                  VARCHAR(200) NOT NULL,
    token_endpoint_auth_method   VARCHAR(30) NOT NULL DEFAULT 'none',
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE service.oauth_client_redirect_uris (
    client_id       VARCHAR(160) NOT NULL REFERENCES service.oauth_clients(client_id) ON DELETE CASCADE,
    redirect_uri    VARCHAR(1000) NOT NULL,
    PRIMARY KEY (client_id, redirect_uri)
);

CREATE TABLE service.oauth_authorizations (
    id                      VARCHAR(80) PRIMARY KEY,
    client_id               VARCHAR(160) NOT NULL REFERENCES service.oauth_clients(client_id) ON DELETE CASCADE,
    redirect_uri            VARCHAR(1000) NOT NULL,
    state                   VARCHAR(1000),
    scope                   VARCHAR(500) NOT NULL,
    resource                VARCHAR(1000) NOT NULL,
    code_challenge          VARCHAR(160) NOT NULL,
    code_challenge_method   VARCHAR(10) NOT NULL,
    user_id                 BIGINT REFERENCES service.users(id) ON DELETE CASCADE,
    status                  VARCHAR(20) NOT NULL,
    authorization_code_hash CHAR(64) UNIQUE,
    code_expires_at         TIMESTAMPTZ,
    consumed_at             TIMESTAMPTZ,
    expires_at              TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_oauth_authorizations_code_hash
    ON service.oauth_authorizations(authorization_code_hash)
    WHERE authorization_code_hash IS NOT NULL;

CREATE TABLE service.oauth_tokens (
    id                  UUID PRIMARY KEY,
    client_id           VARCHAR(160) NOT NULL REFERENCES service.oauth_clients(client_id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES service.users(id) ON DELETE CASCADE,
    scope               VARCHAR(500) NOT NULL,
    resource            VARCHAR(1000) NOT NULL,
    access_token_hash   CHAR(64) NOT NULL UNIQUE,
    refresh_token_hash  CHAR(64) NOT NULL UNIQUE,
    access_expires_at   TIMESTAMPTZ NOT NULL,
    refresh_expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_oauth_tokens_user_created
    ON service.oauth_tokens(user_id, created_at DESC);
