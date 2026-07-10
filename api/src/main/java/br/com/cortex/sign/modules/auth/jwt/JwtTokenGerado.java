package br.com.cortex.sign.modules.auth.jwt;

import java.time.LocalDateTime;

public record JwtTokenGerado(
        String token,
        LocalDateTime expiraEm
) {
}
