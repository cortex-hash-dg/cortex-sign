package br.com.cortex.sign.modules.auth.repository;

import br.com.cortex.sign.modules.auth.entity.TokenConfirmacaoEmail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenConfirmacaoEmailRepository extends JpaRepository<TokenConfirmacaoEmail, UUID> {

    Optional<TokenConfirmacaoEmail> findByTokenHash(String tokenHash);

    List<TokenConfirmacaoEmail> findAllByUsuarioIdAndUsadoEmIsNull(UUID usuarioId);
}
