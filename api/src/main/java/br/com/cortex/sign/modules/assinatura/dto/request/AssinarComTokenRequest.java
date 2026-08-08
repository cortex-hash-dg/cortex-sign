package br.com.cortex.sign.modules.assinatura.dto.request;

import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;

public record AssinarComTokenRequest(
        String codigo,

        TipoAssinatura tipo,

        String assinaturaManuscritaBase64,

        String tokenAcessoExterno
) {
}
