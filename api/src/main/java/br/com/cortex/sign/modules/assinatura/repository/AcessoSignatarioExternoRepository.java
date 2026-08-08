package br.com.cortex.sign.modules.assinatura.repository;

import br.com.cortex.sign.modules.assinatura.entity.AcessoSignatarioExterno;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcessoSignatarioExternoRepository extends JpaRepository<AcessoSignatarioExterno, UUID> {

    Optional<AcessoSignatarioExterno> findTopBySolicitacaoAssinaturaIdAndCpfHashOrderByCriadoEmDesc(UUID solicitacaoId, String cpfHash);

    Optional<AcessoSignatarioExterno> findBySolicitacaoAssinaturaIdAndTokenAcessoHashAndTokenExpiraEmAfter(UUID solicitacaoId, String tokenAcessoHash, LocalDateTime agora);
}
