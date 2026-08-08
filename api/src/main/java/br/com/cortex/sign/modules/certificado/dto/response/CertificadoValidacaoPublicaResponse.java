package br.com.cortex.sign.modules.certificado.dto.response;

import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusAssinatura;
import java.time.LocalDateTime;
import java.util.UUID;

public record CertificadoValidacaoPublicaResponse(
        UUID certificadoId,
        UUID documentoId,
        UUID assinaturaId,
        StatusAssinatura status,
        TipoProvedorAssinatura provedor,
        String protocolo,
        LocalDateTime assinadoEm,
        String hashDocumento,
        String assinaturaDigital,
        LocalDateTime certificadoCriadoEm
) {
}
