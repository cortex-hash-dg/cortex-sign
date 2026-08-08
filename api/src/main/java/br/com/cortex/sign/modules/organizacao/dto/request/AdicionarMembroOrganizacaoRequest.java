package br.com.cortex.sign.modules.organizacao.dto.request;

import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdicionarMembroOrganizacaoRequest(
        @NotNull(message = "O usuário é obrigatório")
        UUID usuarioId,

        @NotNull(message = "O papel é obrigatório")
        PapelOrganizacao papel
) {
}
