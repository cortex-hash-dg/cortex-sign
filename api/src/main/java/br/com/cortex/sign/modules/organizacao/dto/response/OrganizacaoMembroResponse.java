package br.com.cortex.sign.modules.organizacao.dto.response;

import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusMembroOrganizacao;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizacaoMembroResponse(
        UUID id,
        UUID usuarioId,
        String usuarioNome,
        String usuarioEmail,
        UUID organizacaoId,
        String organizacaoNome,
        PapelOrganizacao papel,
        StatusMembroOrganizacao status,
        LocalDateTime convidadoEm,
        LocalDateTime aceitoEm,
        LocalDateTime suspensoEm,
        LocalDateTime removidoEm,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
