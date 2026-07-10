CREATE TABLE tokens_atualizacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expira_em TIMESTAMP NOT NULL,
    revogado_em TIMESTAMP,
    ultimo_uso_em TIMESTAMP,
    ip_criacao VARCHAR(80),
    user_agent VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_tokens_atualizacao_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
);

CREATE INDEX idx_tokens_atualizacao_usuario
    ON tokens_atualizacao (usuario_id);

CREATE INDEX idx_tokens_atualizacao_token_hash
    ON tokens_atualizacao (token_hash);

CREATE INDEX idx_tokens_atualizacao_status
    ON tokens_atualizacao (expira_em, revogado_em);
