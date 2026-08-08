package br.com.cortex.sign.modules.auth.dto.response;

import java.time.LocalDateTime;

public record ConfirmacaoEmailSolicitadaResponse(
        String email,
        String mensagem,
        LocalDateTime expiraEm
) {
}
