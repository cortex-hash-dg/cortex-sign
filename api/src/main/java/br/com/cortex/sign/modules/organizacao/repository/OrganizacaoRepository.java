package br.com.cortex.sign.modules.organizacao.repository;

import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, UUID> {
}
