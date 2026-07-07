package br.com.cortex.sign.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrganizacaoResponse(
        UUID id,
        String nome,
        String numeroDocumento,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}