package br.com.cortex.sign.modules.certificado.dto.response;

import java.util.UUID;

public record CertificadoDocumentoValidacaoResponse(
        UUID certificadoId,
        boolean certificadoEncontrado,
        boolean documentoPossuiCertificado,
        boolean hashDocumentoConfere,
        boolean hashArquivoAssinadoConfere,
        String hashArquivoEnviadoSha256,
        String hashDocumentoRegistrado,
        String hashDocumentoExtraido,
        String hashArquivoAssinadoRegistrado,
        String assinaturaDigital,
        String mensagem,
        CertificadoValidacaoPublicaResponse certificado
) {
}
