package br.com.cortex.sign.modules.usuario.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoMembroService;
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
    private final OrganizacaoMembroService organizacaoMembroService;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse criar(UsuarioAutenticado usuarioAutenticado, CriarUsuarioRequest request) {
        Usuario usuarioLogado = buscarUsuarioAutenticado(usuarioAutenticado);
        validarPerfilPermitidoParaCriacao(usuarioLogado, request.perfil());

        String emailNormalizado = normalizarEmail(request.email());
        validarEmailDisponivel(emailNormalizado);

        Organizacao organizacao = resolverOrganizacaoParaEscrita(usuarioLogado, request.organizacaoId(), request.perfil());
        String senhaHash = passwordEncoder.encode(request.senha());
        Usuario usuario = usuarioMapper.toEntity(request, organizacao, senhaHash);
        usuario.setEmail(emailNormalizado);

        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);
        organizacaoMembroService.criarVinculoInicialSeNecessario(usuarioSalvo, organizacao, request.perfil());

        return usuarioMapper.toResponse(usuarioSalvo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuarioLogado = buscarUsuarioAutenticado(usuarioAutenticado);

        List<Usuario> usuarios = isSuperAdmin(usuarioLogado)
                ? usuarioRepository.findAllByAtivoTrueOrderByNomeAsc()
                : usuarioRepository.findAllByOrganizacaoIdAndAtivoTrueOrderByNomeAsc(obterOrganizacaoIdObrigatoria(usuarioLogado));

        return usuarios
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuarioLogado = buscarUsuarioAutenticado(usuarioAutenticado);
        Usuario usuario = buscarEntidadePorId(id);
        validarAcessoAoUsuario(usuarioLogado, usuario);

        return usuarioMapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(UsuarioAutenticado usuarioAutenticado, UUID id, AtualizarUsuarioRequest request) {
        Usuario usuarioLogado = buscarUsuarioAutenticado(usuarioAutenticado);
        Usuario usuario = buscarEntidadePorId(id);
        validarAcessoAoUsuario(usuarioLogado, usuario);
        validarPerfilPermitidoParaCriacao(usuarioLogado, request.perfil());

        String emailNormalizado = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, id)) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }

        Organizacao organizacao = resolverOrganizacaoParaEscrita(usuarioLogado, request.organizacaoId(), request.perfil());
        usuarioMapper.updateEntity(usuario, request, organizacao);
        usuario.setEmail(emailNormalizado);

        Usuario usuarioAtualizado = usuarioRepository.saveAndFlush(usuario);
        organizacaoMembroService.criarVinculoInicialSeNecessario(usuarioAtualizado, organizacao, request.perfil());

        return usuarioMapper.toResponse(usuarioAtualizado);
    }

    @Transactional
    public void excluir(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuarioLogado = buscarUsuarioAutenticado(usuarioAutenticado);
        Usuario usuario = buscarEntidadePorId(id);
        validarAcessoAoUsuario(usuarioLogado, usuario);

        if (usuario.getId().equals(usuarioLogado.getId())) {
            throw new ConflitoException("Você não pode excluir o próprio usuário");
        }

        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
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

    private Organizacao resolverOrganizacaoParaEscrita(Usuario usuarioLogado, UUID organizacaoIdSolicitada, PerfilUsuario perfil) {
        if (isSuperAdmin(usuarioLogado)) {
            if (perfil != PerfilUsuario.SUPER_ADMINISTRADOR && organizacaoIdSolicitada == null) {
                throw new ConflitoException("A organização é obrigatória para este perfil de usuário");
            }

            return buscarOrganizacaoOpcional(organizacaoIdSolicitada);
        }

        UUID organizacaoIdUsuarioLogado = obterOrganizacaoIdObrigatoria(usuarioLogado);
        if (organizacaoIdSolicitada != null && !organizacaoIdSolicitada.equals(organizacaoIdUsuarioLogado)) {
            throw new AcessoNegadoException("Você não pode gerenciar usuários de outra organização");
        }

        if (perfil == PerfilUsuario.SUPER_ADMINISTRADOR) {
            throw new AcessoNegadoException("Administrador da organização não pode criar Super Admin");
        }

        return buscarOrganizacaoOpcional(organizacaoIdUsuarioLogado);
    }

    private void validarPerfilPermitidoParaCriacao(Usuario usuarioLogado, PerfilUsuario perfil) {
        if (isSuperAdmin(usuarioLogado)) {
            return;
        }

        if (usuarioLogado.getPerfil() != PerfilUsuario.ADMINISTRADOR_ORGANIZACAO) {
            throw new AcessoNegadoException("Você não tem permissão para gerenciar usuários");
        }

        if (perfil == PerfilUsuario.SUPER_ADMINISTRADOR) {
            throw new AcessoNegadoException("Administrador da organização não pode criar Super Admin");
        }
    }

    private void validarAcessoAoUsuario(Usuario usuarioLogado, Usuario usuarioAlvo) {
        if (isSuperAdmin(usuarioLogado)) {
            return;
        }

        UUID organizacaoIdUsuarioLogado = obterOrganizacaoIdObrigatoria(usuarioLogado);
        if (usuarioAlvo.getOrganizacao() == null || !usuarioAlvo.getOrganizacao().getId().equals(organizacaoIdUsuarioLogado)) {
            throw new AcessoNegadoException("Você não tem permissão para acessar usuários de outra organização");
        }
    }

    private Usuario buscarUsuarioAutenticado(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }

        Usuario usuario = usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        return usuario;
    }

    private boolean isSuperAdmin(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.SUPER_ADMINISTRADOR;
    }

    private UUID obterOrganizacaoIdObrigatoria(Usuario usuario) {
        if (usuario.getOrganizacao() == null) {
            throw new ConflitoException("A organização é obrigatória para este perfil de usuário");
        }

        return usuario.getOrganizacao().getId();
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
