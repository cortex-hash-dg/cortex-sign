package br.com.cortex.sign.modules.auth.service;

import br.com.cortex.sign.modules.usuario.entity.Usuario;

public record TokenAtualizacaoRotacionado(
        Usuario usuario,
        TokenAtualizacaoGerado tokenAtualizacao
) {
}
