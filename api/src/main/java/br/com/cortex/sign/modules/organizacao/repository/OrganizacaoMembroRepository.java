package br.com.cortex.sign.modules.organizacao.repository;

import br.com.cortex.sign.modules.organizacao.entity.OrganizacaoMembro;
import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusMembroOrganizacao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoMembroRepository extends JpaRepository<OrganizacaoMembro, UUID> {

    List<OrganizacaoMembro> findAllByOrganizacaoIdOrderByCriadoEmAsc(UUID organizacaoId);

    List<OrganizacaoMembro> findAllByUsuarioIdOrderByCriadoEmAsc(UUID usuarioId);

    Optional<OrganizacaoMembro> findByOrganizacaoIdAndUsuarioId(UUID organizacaoId, UUID usuarioId);

    boolean existsByOrganizacaoIdAndUsuarioId(UUID organizacaoId, UUID usuarioId);

    boolean existsByOrganizacaoIdAndPapelAndStatus(UUID organizacaoId, PapelOrganizacao papel, StatusMembroOrganizacao status);
}
