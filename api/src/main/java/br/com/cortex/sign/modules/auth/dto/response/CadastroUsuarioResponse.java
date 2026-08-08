package br.com.cortex.sign.modules.auth.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record CadastroUsuarioResponse(
        UUID id,
        String nome,
        String email,
        String cpf,
        String telefone,
        String nomeSocial,
        LocalDate dataNascimento,
        Boolean emailVerificado,
        Boolean telefoneVerificado
) {
}
