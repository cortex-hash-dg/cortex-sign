CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizacao_id UUID,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuarios_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES organizacoes (id)
);

CREATE INDEX idx_usuarios_organizacao_id ON usuarios (organizacao_id);
CREATE INDEX idx_usuarios_email ON usuarios (email);
