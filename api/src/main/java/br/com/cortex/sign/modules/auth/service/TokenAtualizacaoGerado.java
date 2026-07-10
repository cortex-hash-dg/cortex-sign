package br.com.cortex.sign.modules.auth.service;

import java.time.LocalDateTime;

public record TokenAtualizacaoGerado(
        String token,
        LocalDateTime expiraEm
) {
}
