package br.com.cortex.sign.integration.assinatura.dto;

import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import java.time.LocalDateTime;

public record ResultadoAssinatura(
        TipoProvedorAssinatura provedor,
        String protocolo,
        LocalDateTime assinadoEm,
        String metadados,
        String evidenciaHashSha256,
        String termoAceite
) {
}
