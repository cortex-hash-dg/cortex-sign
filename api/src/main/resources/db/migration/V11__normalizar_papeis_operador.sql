UPDATE usuarios
SET perfil = 'OPERADOR'
WHERE perfil = 'SIGNATARIO';

UPDATE organizacoes_membros
SET papel = 'OPERADOR'
WHERE papel IN ('MEMBRO', 'LIMITADO');
