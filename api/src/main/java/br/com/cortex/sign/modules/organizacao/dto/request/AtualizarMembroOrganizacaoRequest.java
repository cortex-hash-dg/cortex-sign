package br.com.cortex.sign.modules.organizacao.dto.request;

import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusMembroOrganizacao;
import jakarta.validation.constraints.NotNull;

public record AtualizarMembroOrganizacaoRequest(
        @NotNull(message = "O papel é obrigatório")
        PapelOrganizacao papel,

        @NotNull(message = "O status é obrigatório")
        StatusMembroOrganizacao status,

        String motivoSuspensao
) {
}
