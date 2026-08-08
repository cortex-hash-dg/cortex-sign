package br.com.cortex.sign.modules.assinatura.dto.response;

import java.time.LocalDateTime;

public record AcessoSignatarioExternoResponse(
        String tokenAcesso,
        LocalDateTime expiraEm,
        String mensagem
) {
}
