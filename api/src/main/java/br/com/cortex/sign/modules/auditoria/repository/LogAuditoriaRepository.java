package br.com.cortex.sign.modules.auditoria.repository;

import br.com.cortex.sign.modules.auditoria.entity.LogAuditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    List<LogAuditoria> findTop100ByOrderByCriadoEmDesc();

    List<LogAuditoria> findTop100ByOrganizacaoIdOrderByCriadoEmDesc(UUID organizacaoId);
}
