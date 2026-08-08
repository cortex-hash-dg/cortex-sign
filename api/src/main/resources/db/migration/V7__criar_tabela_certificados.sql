CREATE TABLE certificados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    assinatura_id UUID NOT NULL,
    signatario_final_id UUID NOT NULL,
    hash_documento VARCHAR(64) NOT NULL,
    assinatura_digital TEXT NOT NULL,
    dados_autenticacao TEXT,
    url_documento VARCHAR(500),
    url_verificacao VARCHAR(500) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_certificados_documento
        FOREIGN KEY (documento_id)
        REFERENCES documentos (id),

    CONSTRAINT fk_certificados_assinatura
        FOREIGN KEY (assinatura_id)
        REFERENCES assinaturas (id),

    CONSTRAINT fk_certificados_signatario_final
        FOREIGN KEY (signatario_final_id)
        REFERENCES signatarios (id)
);

CREATE INDEX idx_certificados_documento
    ON certificados (documento_id);

CREATE INDEX idx_certificados_assinatura
    ON certificados (assinatura_id);

CREATE INDEX idx_certificados_signatario_final
    ON certificados (signatario_final_id);

CREATE INDEX idx_certificados_hash_documento
    ON certificados (hash_documento);
