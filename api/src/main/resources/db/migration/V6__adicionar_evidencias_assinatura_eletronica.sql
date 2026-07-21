ALTER TABLE assinaturas
    ADD COLUMN documento_hash_sha256 VARCHAR(64),
    ADD COLUMN evidencia_hash_sha256 VARCHAR(64),
    ADD COLUMN termo_aceite TEXT;

CREATE INDEX idx_assinaturas_evidencia_hash
    ON assinaturas (evidencia_hash_sha256);
