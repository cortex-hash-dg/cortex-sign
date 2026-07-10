package br.com.cortex.sign.modules.documento.repository;

import br.com.cortex.sign.modules.documento.entity.ArquivoArmazenado;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArquivoArmazenadoRepository extends JpaRepository<ArquivoArmazenado, UUID> {
}
