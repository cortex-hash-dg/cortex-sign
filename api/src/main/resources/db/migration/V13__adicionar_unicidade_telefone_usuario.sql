CREATE UNIQUE INDEX IF NOT EXISTS uk_usuarios_telefone
    ON usuarios (telefone)
    WHERE telefone IS NOT NULL;
