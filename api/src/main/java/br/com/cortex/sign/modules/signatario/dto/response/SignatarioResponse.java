package br.com.cortex.sign.modules.signatario.dto.response;

import br.com.cortex.sign.modules.signatario.enums.TipoSignatario;
import java.time.LocalDateTime;
import java.util.UUID;

public record SignatarioResponse(
        UUID id,
        UUID documentoId,
        String documentoTitulo,
        String nome,
        String email,
        String numeroDocumento,
        String telefone,
        TipoSignatario tipo,
        Integer ordemAssinatura,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
