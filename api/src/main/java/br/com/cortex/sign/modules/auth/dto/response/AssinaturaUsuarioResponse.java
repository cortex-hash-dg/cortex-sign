package br.com.cortex.sign.modules.auth.dto.response;

public record AssinaturaUsuarioResponse(
        String nomeAssinatura,
        String assinaturaManuscritaBase64,
        boolean possuiAssinaturaManuscrita
) {
}
