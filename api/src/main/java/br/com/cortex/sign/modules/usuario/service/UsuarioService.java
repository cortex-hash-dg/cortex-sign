package br.com.cortex.sign.modules.usuario.service;

import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoService;
import br.com.cortex.sign.modules.usuario.dto.request.AtualizarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.request.CriarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.response.UsuarioResponse;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.mapper.UsuarioMapper;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoService organizacaoService;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        validarOrganizacaoObrigatoria(request.perfil(), request.organizacaoId());
        String emailNormalizado = normalizarEmail(request.email());
        validarEmailDisponivel(emailNormalizado);

        Organizacao organizacao = buscarOrganizacaoOpcional(request.organizacaoId());
        String senhaHash = passwordEncoder.encode(request.senha());
        Usuario usuario = usuarioMapper.toEntity(request, organizacao, senhaHash);
        usuario.setEmail(emailNormalizado);

        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);

        return usuarioMapper.toResponse(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return usuarioMapper.toResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, AtualizarUsuarioRequest request) {
        validarOrganizacaoObrigatoria(request.perfil(), request.organizacaoId());

        Usuario usuario = buscarEntidadePorId(id);

        String emailNormalizado = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, id)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }

        Organizacao organizacao = buscarOrganizacaoOpcional(request.organizacaoId());
        usuarioMapper.updateEntity(usuario, request, organizacao);
        usuario.setEmail(emailNormalizado);

        Usuario usuarioAtualizado = usuarioRepository.saveAndFlush(usuario);

        return usuarioMapper.toResponse(usuarioAtualizado);
    }

    @Transactional
    public void excluir(UUID id) {
        Usuario usuario = buscarEntidadePorId(id);

        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidadePorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private Organizacao buscarOrganizacaoOpcional(UUID organizacaoId) {
        if (organizacaoId == null) {
            return null;
        }

        return organizacaoService.buscarEntidadePorId(organizacaoId);
    }

    private void validarOrganizacaoObrigatoria(PerfilUsuario perfil, UUID organizacaoId) {
        if (perfil != PerfilUsuario.SUPER_ADMINISTRADOR && organizacaoId == null) {
            throw new ConflitoException("A organização é obrigatória para este perfil de usuário");
        }
    }

    private void validarEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
