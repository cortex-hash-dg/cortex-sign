CREATE TABLE signatarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    numero_documento VARCHAR(30),
    tipo VARCHAR(40) NOT NULL,
    ordem_assinatura INTEGER NOT NULL DEFAULT 1,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_signatarios_documento
        FOREIGN KEY (documento_id)
        REFERENCES documentos (id)
);

CREATE TABLE solicitacoes_assinatura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    signatario_id UUID NOT NULL,
    status VARCHAR(40) NOT NULL,
    token VARCHAR(120) NOT NULL UNIQUE,
    codigo_hash VARCHAR(128) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_solicitacoes_assinatura_documento
        FOREIGN KEY (documento_id)
        REFERENCES documentos (id),

    CONSTRAINT fk_solicitacoes_assinatura_signatario
        FOREIGN KEY (signatario_id)
        REFERENCES signatarios (id)
);

CREATE TABLE assinaturas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_assinatura_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provedor VARCHAR(60) NOT NULL,
    protocolo VARCHAR(120),
    assinado_em TIMESTAMP,
    rejeitado_em TIMESTAMP,
    motivo_rejeicao VARCHAR(500),
    ip_assinatura VARCHAR(80),
    user_agent VARCHAR(500),
    metadados TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assinaturas_solicitacao
        FOREIGN KEY (solicitacao_assinatura_id)
        REFERENCES solicitacoes_assinatura (id)
);

CREATE TABLE logs_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizacao_id UUID,
    usuario_id UUID,
    acao VARCHAR(100) NOT NULL,
    entidade_tipo VARCHAR(80) NOT NULL,
    entidade_id UUID,
    detalhes TEXT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_logs_auditoria_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES organizacoes (id),

    CONSTRAINT fk_logs_auditoria_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id)
);

CREATE INDEX idx_signatarios_documento
    ON signatarios (documento_id);

CREATE INDEX idx_signatarios_email
    ON signatarios (email);

CREATE INDEX idx_solicitacoes_assinatura_documento
    ON solicitacoes_assinatura (documento_id);

CREATE INDEX idx_solicitacoes_assinatura_signatario
    ON solicitacoes_assinatura (signatario_id);

CREATE INDEX idx_solicitacoes_assinatura_token
    ON solicitacoes_assinatura (token);

CREATE INDEX idx_solicitacoes_assinatura_status
    ON solicitacoes_assinatura (status);

CREATE INDEX idx_assinaturas_solicitacao
    ON assinaturas (solicitacao_assinatura_id);

CREATE INDEX idx_logs_auditoria_organizacao
    ON logs_auditoria (organizacao_id);

CREATE INDEX idx_logs_auditoria_usuario
    ON logs_auditoria (usuario_id);

CREATE INDEX idx_logs_auditoria_entidade
    ON logs_auditoria (entidade_tipo, entidade_id);
