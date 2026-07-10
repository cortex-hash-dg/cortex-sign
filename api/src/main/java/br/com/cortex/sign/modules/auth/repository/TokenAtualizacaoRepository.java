package br.com.cortex.sign.modules.auth.repository;

import br.com.cortex.sign.modules.auth.entity.TokenAtualizacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenAtualizacaoRepository extends JpaRepository<TokenAtualizacao, UUID> {

    Optional<TokenAtualizacao> findByTokenHash(String tokenHash);

    List<TokenAtualizacao> findAllByUsuarioIdAndRevogadoEmIsNull(UUID usuarioId);
}
