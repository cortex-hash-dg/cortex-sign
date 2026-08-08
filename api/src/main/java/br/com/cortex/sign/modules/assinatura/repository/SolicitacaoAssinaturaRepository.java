package br.com.cortex.sign.modules.assinatura.repository;

import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitacaoAssinaturaRepository extends JpaRepository<SolicitacaoAssinatura, UUID> {

    List<SolicitacaoAssinatura> findAllByDocumentoIdOrderByCriadoEmDesc(UUID documentoId);

    List<SolicitacaoAssinatura> findAllByDocumentoIdOrderByCriadoEmAsc(UUID documentoId);

    Optional<SolicitacaoAssinatura> findByToken(String token);

    boolean existsBySignatarioIdAndStatus(UUID signatarioId, StatusSolicitacaoAssinatura status);

    long countByDocumentoIdAndStatus(UUID documentoId, StatusSolicitacaoAssinatura status);
}
