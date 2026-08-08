package br.com.cortex.sign.modules.usuario.repository;

import br.com.cortex.sign.modules.usuario.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario> findAllByAtivoTrueOrderByNomeAsc();

    List<Usuario> findAllByOrganizacaoIdAndAtivoTrueOrderByNomeAsc(UUID organizacaoId);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByCpfAndIdNot(String cpf, UUID id);

    boolean existsByTelefoneAndIdNot(String telefone, UUID id);
}
