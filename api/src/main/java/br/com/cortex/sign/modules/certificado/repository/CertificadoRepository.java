package br.com.cortex.sign.modules.certificado.repository;

import br.com.cortex.sign.modules.certificado.entity.Certificado;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {

    Optional<Certificado> findTopByDocumentoIdOrderByCriadoEmDesc(UUID documentoId);
}
