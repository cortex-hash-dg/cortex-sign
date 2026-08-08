CREATE TABLE acessos_signatario_externo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    solicitacao_assinatura_id UUID NOT NULL,
    cpf_hash VARCHAR(128) NOT NULL,
    codigo_hash VARCHAR(128) NOT NULL,
    token_acesso_hash VARCHAR(128),
    canal VARCHAR(20) NOT NULL,
    destino_mascarado VARCHAR(150),
    codigo_expira_em TIMESTAMP NOT NULL,
    token_expira_em TIMESTAMP,
    validado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_acessos_signatario_externo_solicitacao
        FOREIGN KEY (solicitacao_assinatura_id)
        REFERENCES solicitacoes_assinatura (id)
);

CREATE INDEX idx_acessos_signatario_externo_solicitacao
    ON acessos_signatario_externo (solicitacao_assinatura_id);

CREATE INDEX idx_acessos_signatario_externo_cpf
    ON acessos_signatario_externo (cpf_hash);

CREATE INDEX idx_acessos_signatario_externo_token
    ON acessos_signatario_externo (token_acesso_hash);
