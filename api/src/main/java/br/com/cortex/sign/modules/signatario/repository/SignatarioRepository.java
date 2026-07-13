package br.com.cortex.sign.modules.signatario.repository;

import br.com.cortex.sign.modules.signatario.entity.Signatario;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignatarioRepository extends JpaRepository<Signatario, UUID> {

    List<Signatario> findAllByDocumentoIdOrderByOrdemAssinaturaAscCriadoEmAsc(UUID documentoId);

    boolean existsByDocumentoIdAndEmailIgnoreCase(UUID documentoId, String email);

    boolean existsByDocumentoIdAndEmailIgnoreCaseAndIdNot(UUID documentoId, String email, UUID id);
}
