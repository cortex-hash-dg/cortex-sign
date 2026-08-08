ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS cpf VARCHAR(11),
    ADD COLUMN IF NOT EXISTS telefone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS nome_social VARCHAR(150),
    ADD COLUMN IF NOT EXISTS data_nascimento DATE,
    ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS telefone_verificado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_cpf
    ON usuarios (cpf)
    WHERE cpf IS NOT NULL;

ALTER TABLE organizacoes
    ADD COLUMN IF NOT EXISTS razao_social VARCHAR(180),
    ADD COLUMN IF NOT EXISTS nome_fantasia VARCHAR(150),
    ADD COLUMN IF NOT EXISTS cnpj VARCHAR(14),
    ADD COLUMN IF NOT EXISTS email_corporativo VARCHAR(150),
    ADD COLUMN IF NOT EXISTS telefone VARCHAR(30),
    ADD COLUMN IF NOT EXISTS cep VARCHAR(20),
    ADD COLUMN IF NOT EXISTS logradouro VARCHAR(180),
    ADD COLUMN IF NOT EXISTS numero VARCHAR(20),
    ADD COLUMN IF NOT EXISTS complemento VARCHAR(120),
    ADD COLUMN IF NOT EXISTS bairro VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cidade VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estado VARCHAR(2),
    ADD COLUMN IF NOT EXISTS pais VARCHAR(80),
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS status VARCHAR(40) NOT NULL DEFAULT 'ATIVA',
    ADD COLUMN IF NOT EXISTS proprietario_usuario_id UUID,
    ADD COLUMN IF NOT EXISTS plano VARCHAR(80),
    ADD COLUMN IF NOT EXISTS status_verificacao VARCHAR(40) NOT NULL DEFAULT 'PENDENTE';

CREATE UNIQUE INDEX IF NOT EXISTS uk_organizacoes_cnpj
    ON organizacoes (cnpj)
    WHERE cnpj IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_organizacoes_proprietario_usuario'
    ) THEN
        ALTER TABLE organizacoes
            ADD CONSTRAINT fk_organizacoes_proprietario_usuario
                FOREIGN KEY (proprietario_usuario_id)
                REFERENCES usuarios (id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS organizacoes_membros (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    organizacao_id UUID NOT NULL,
    papel VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'ATIVO',
    convidado_por_usuario_id UUID,
    papel_alterado_por_usuario_id UUID,
    grupos TEXT,
    permissoes_adicionais TEXT,
    permissoes_removidas TEXT,
    motivo_suspensao VARCHAR(500),
    convidado_em TIMESTAMP,
    aceito_em TIMESTAMP,
    papel_alterado_em TIMESTAMP,
    suspenso_em TIMESTAMP,
    removido_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_organizacoes_membros_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT fk_organizacoes_membros_organizacao
        FOREIGN KEY (organizacao_id)
        REFERENCES organizacoes (id),

    CONSTRAINT fk_organizacoes_membros_convidado_por
        FOREIGN KEY (convidado_por_usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT fk_organizacoes_membros_papel_alterado_por
        FOREIGN KEY (papel_alterado_por_usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT uk_organizacoes_membros_usuario_organizacao
        UNIQUE (usuario_id, organizacao_id)
);

CREATE INDEX IF NOT EXISTS idx_organizacoes_membros_usuario
    ON organizacoes_membros (usuario_id);

CREATE INDEX IF NOT EXISTS idx_organizacoes_membros_organizacao
    ON organizacoes_membros (organizacao_id);

CREATE INDEX IF NOT EXISTS idx_organizacoes_membros_papel_status
    ON organizacoes_membros (organizacao_id, papel, status);

WITH usuarios_ordenados AS (
    SELECT
        id,
        organizacao_id,
        perfil,
        ROW_NUMBER() OVER (
            PARTITION BY organizacao_id
            ORDER BY
                CASE perfil
                    WHEN 'ADMINISTRADOR_ORGANIZACAO' THEN 0
                    WHEN 'SUPER_ADMINISTRADOR' THEN 1
                    ELSE 2
                END,
                criado_em ASC
        ) AS ordem_na_organizacao
    FROM usuarios
    WHERE organizacao_id IS NOT NULL
)
INSERT INTO organizacoes_membros (
    usuario_id,
    organizacao_id,
    papel,
    status,
    aceito_em
)
SELECT
    id,
    organizacao_id,
    CASE
        WHEN perfil = 'AUDITOR' THEN 'AUDITOR'
        WHEN perfil = 'ADMINISTRADOR_ORGANIZACAO' AND ordem_na_organizacao = 1 THEN 'PROPRIETARIO'
        WHEN perfil IN ('ADMINISTRADOR_ORGANIZACAO', 'SUPER_ADMINISTRADOR') THEN 'ADMINISTRADOR'
        ELSE 'MEMBRO'
    END,
    'ATIVO',
    CURRENT_TIMESTAMP
FROM usuarios_ordenados
ON CONFLICT (usuario_id, organizacao_id) DO NOTHING;

WITH proprietarios AS (
    SELECT DISTINCT ON (organizacao_id)
        organizacao_id,
        usuario_id
    FROM organizacoes_membros
    WHERE papel = 'PROPRIETARIO'
      AND status = 'ATIVO'
    ORDER BY organizacao_id, criado_em ASC
)
UPDATE organizacoes organizacao
SET proprietario_usuario_id = proprietarios.usuario_id
FROM proprietarios
WHERE organizacao.id = proprietarios.organizacao_id
  AND organizacao.proprietario_usuario_id IS NULL;
