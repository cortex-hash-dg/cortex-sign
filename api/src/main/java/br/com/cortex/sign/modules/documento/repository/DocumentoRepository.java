package br.com.cortex.sign.modules.documento.repository;

import br.com.cortex.sign.modules.documento.entity.Documento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findAllByOrderByCriadoEmDesc();

    List<Documento> findAllByOrganizacaoIdOrderByCriadoEmDesc(UUID organizacaoId);

    Optional<Documento> findByIdAndOrganizacaoId(UUID id, UUID organizacaoId);
}
