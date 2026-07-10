CREATE TABLE arquivos_armazenados (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provedor VARCHAR(50) NOT NULL,
    nome_original VARCHAR(255) NOT NULL,
    nome_armazenado VARCHAR(255) NOT NULL,
    caminho VARCHAR(500) NOT NULL,
    tipo_conteudo VARCHAR(100),
    tamanho_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documentos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizacao_id UUID NOT NULL,
    arquivo_atual_id UUID NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    status VARCHAR(40) NOT NULL,
    criado_por_usuario_id UUID NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documentos_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES organizacoes (id),

    CONSTRAINT fk_documentos_arquivo_atual
        FOREIGN KEY (arquivo_atual_id)
        REFERENCES arquivos_armazenados (id),

    CONSTRAINT fk_documentos_criado_por_usuario
        FOREIGN KEY (criado_por_usuario_id)
        REFERENCES usuarios (id)
);

CREATE TABLE documentos_versoes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    documento_id UUID NOT NULL,
    arquivo_id UUID NOT NULL,
    numero_versao INTEGER NOT NULL,
    criado_por_usuario_id UUID NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_documentos_versoes_documento
        FOREIGN KEY (documento_id)
        REFERENCES documentos (id),

    CONSTRAINT fk_documentos_versoes_arquivo
        FOREIGN KEY (arquivo_id)
        REFERENCES arquivos_armazenados (id),

    CONSTRAINT fk_documentos_versoes_criado_por_usuario
        FOREIGN KEY (criado_por_usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT uk_documentos_versoes_documento_numero
        UNIQUE (documento_id, numero_versao)
);

CREATE INDEX idx_arquivos_armazenados_provedor
    ON arquivos_armazenados (provedor);

CREATE INDEX idx_documentos_organizacao
    ON documentos (organizacao_id);

CREATE INDEX idx_documentos_status
    ON documentos (status);

CREATE INDEX idx_documentos_criado_por_usuario
    ON documentos (criado_por_usuario_id);

CREATE INDEX idx_documentos_versoes_documento
    ON documentos_versoes (documento_id);
