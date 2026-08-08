CREATE TABLE tokens_confirmacao_email (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expira_em TIMESTAMP NOT NULL,
    usado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tokens_confirmacao_email_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
);

CREATE INDEX idx_tokens_confirmacao_email_usuario
    ON tokens_confirmacao_email (usuario_id);

CREATE INDEX idx_tokens_confirmacao_email_token_hash
    ON tokens_confirmacao_email (token_hash);
