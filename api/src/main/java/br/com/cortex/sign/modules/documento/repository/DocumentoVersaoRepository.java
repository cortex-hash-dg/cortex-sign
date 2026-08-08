package br.com.cortex.sign.modules.documento.repository;

import br.com.cortex.sign.modules.documento.entity.DocumentoVersao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoVersaoRepository extends JpaRepository<DocumentoVersao, UUID> {

    List<DocumentoVersao> findAllByDocumentoIdOrderByNumeroVersaoAsc(UUID documentoId);

    DocumentoVersao findTopByDocumentoIdOrderByNumeroVersaoDesc(UUID documentoId);
}
