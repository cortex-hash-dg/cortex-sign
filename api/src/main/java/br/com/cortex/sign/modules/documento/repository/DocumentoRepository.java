package br.com.cortex.sign.modules.documento.repository;

import br.com.cortex.sign.modules.documento.entity.Documento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findAllByOrderByCriadoEmDesc();

    List<Documento> findAllByOrganizacaoIdOrderByCriadoEmDesc(UUID organizacaoId);

    Optional<Documento> findByIdAndOrganizacaoId(UUID id, UUID organizacaoId);

    @Query("""
            select distinct documento
            from Documento documento
            left join Signatario signatario on signatario.documento = documento
            where documento.organizacao.id = :organizacaoId
              and (
                documento.criadoPorUsuario.id = :usuarioId
                or lower(signatario.email) = lower(:email)
                or (signatario.numeroDocumento is not null and signatario.numeroDocumento = :cpf)
              )
            order by documento.criadoEm desc
            """)
    List<Documento> findDocumentosVisiveisParaUsuario(
            @Param("organizacaoId") UUID organizacaoId,
            @Param("usuarioId") UUID usuarioId,
            @Param("email") String email,
            @Param("cpf") String cpf
    );
}
