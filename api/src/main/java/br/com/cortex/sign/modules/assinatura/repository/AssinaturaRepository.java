package br.com.cortex.sign.modules.assinatura.repository;

import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<Assinatura, UUID> {

    Optional<Assinatura> findBySolicitacaoAssinaturaId(UUID solicitacaoAssinaturaId);
}
