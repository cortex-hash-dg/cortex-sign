package br.com.cortex.sign.modules.organizacao.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.dto.request.AdicionarMembroOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarMembroOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoMembroResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.entity.OrganizacaoMembro;
import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusMembroOrganizacao;
import br.com.cortex.sign.modules.organizacao.mapper.OrganizacaoMembroMapper;
import br.com.cortex.sign.modules.organizacao.repository.OrganizacaoMembroRepository;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizacaoMembroService {

    private final OrganizacaoMembroRepository membroRepository;
    private final OrganizacaoService organizacaoService;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoMembroMapper membroMapper;

    @Transactional(readOnly = true)
    public List<OrganizacaoMembroResponse> listarPorOrganizacao(UsuarioAutenticado autenticado, UUID organizacaoId) {
        validarPodeLerMembros(autenticado, organizacaoId);

        return membroRepository.findAllByOrganizacaoIdOrderByCriadoEmAsc(organizacaoId)
                .stream()
                .map(membroMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoMembroResponse> listarPorUsuario(UsuarioAutenticado autenticado, UUID usuarioId) {
        Usuario usuarioAutenticado = buscarUsuarioAutenticado(autenticado);

        if (!isSuperAdministrador(usuarioAutenticado) && !usuarioAutenticado.getId().equals(usuarioId)) {
            throw new AcessoNegadoException("Você não tem permissão para consultar vínculos de outro usuário");
        }

        buscarUsuario(usuarioId);

        return membroRepository.findAllByUsuarioIdOrderByCriadoEmAsc(usuarioId)
                .stream()
                .map(membroMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrganizacaoMembroResponse adicionar(
            UsuarioAutenticado autenticado,
            UUID organizacaoId,
            AdicionarMembroOrganizacaoRequest request
    ) {
        Usuario executor = validarPodeGerenciarMembros(autenticado, organizacaoId);
        validarPapelSensivel(executor, organizacaoId, request.papel(), null);

        Organizacao organizacao = organizacaoService.buscarEntidadePorId(organizacaoId);
        Usuario usuario = buscarUsuario(request.usuarioId());

        if (membroRepository.existsByOrganizacaoIdAndUsuarioId(organizacaoId, usuario.getId())) {
            throw new ConflitoException("Usuário já é membro desta organização");
        }

        if (request.papel() == PapelOrganizacao.PROPRIETARIO
                && membroRepository.existsByOrganizacaoIdAndPapelAndStatus(
                organizacaoId,
                PapelOrganizacao.PROPRIETARIO,
                StatusMembroOrganizacao.ATIVO
        )) {
            throw new ConflitoException("Esta organização já possui um proprietário ativo");
        }

        OrganizacaoMembro membro = novoMembro(usuario, organizacao, request.papel(), StatusMembroOrganizacao.ATIVO);
        membro.setConvidadoPorUsuario(executor);

        return membroMapper.toResponse(membroRepository.saveAndFlush(membro));
    }

    @Transactional
    public OrganizacaoMembroResponse atualizar(
            UsuarioAutenticado autenticado,
            UUID organizacaoId,
            UUID membroId,
            AtualizarMembroOrganizacaoRequest request
    ) {
        Usuario executor = validarPodeGerenciarMembros(autenticado, organizacaoId);
        OrganizacaoMembro membro = buscarMembroDaOrganizacao(organizacaoId, membroId);
        validarPapelSensivel(executor, organizacaoId, request.papel(), membro);

        membro.setPapel(request.papel());
        membro.setStatus(request.status());
        membro.setPapelAlteradoPorUsuario(executor);
        membro.setPapelAlteradoEm(LocalDateTime.now());
        membro.setMotivoSuspensao(normalizarTexto(request.motivoSuspensao()));

        if (request.status() == StatusMembroOrganizacao.SUSPENSO && membro.getSuspensoEm() == null) {
            membro.setSuspensoEm(LocalDateTime.now());
        }

        if (request.status() == StatusMembroOrganizacao.REMOVIDO && membro.getRemovidoEm() == null) {
            membro.setRemovidoEm(LocalDateTime.now());
        }

        return membroMapper.toResponse(membroRepository.saveAndFlush(membro));
    }

    @Transactional
    public void remover(UsuarioAutenticado autenticado, UUID organizacaoId, UUID membroId) {
        Usuario executor = validarPodeGerenciarMembros(autenticado, organizacaoId);
        OrganizacaoMembro membro = buscarMembroDaOrganizacao(organizacaoId, membroId);
        validarPapelSensivel(executor, organizacaoId, membro.getPapel(), membro);

        membro.setStatus(StatusMembroOrganizacao.REMOVIDO);
        membro.setRemovidoEm(LocalDateTime.now());
        membroRepository.save(membro);
    }

    @Transactional
    public void criarVinculoInicialSeNecessario(Usuario usuario, Organizacao organizacao, PerfilUsuario perfil) {
        if (usuario == null || organizacao == null || perfil == null) {
            return;
        }

        membroRepository.findByOrganizacaoIdAndUsuarioId(organizacao.getId(), usuario.getId())
                .ifPresentOrElse(
                        membro -> atualizarPapelCompatibilidade(membro, perfil),
                        () -> membroRepository.save(novoMembro(
                                usuario,
                                organizacao,
                                resolverPapelCompatibilidade(perfil),
                                StatusMembroOrganizacao.ATIVO
                        ))
                );
    }

    private void validarPodeLerMembros(UsuarioAutenticado autenticado, UUID organizacaoId) {
        Usuario usuario = buscarUsuarioAutenticado(autenticado);
        organizacaoService.buscarEntidadePorId(organizacaoId);

        if (isSuperAdministrador(usuario)) {
            return;
        }

        OrganizacaoMembro membro = buscarVinculoAtivo(organizacaoId, usuario.getId());
        if (membro.getPapel() != PapelOrganizacao.PROPRIETARIO
                && membro.getPapel() != PapelOrganizacao.ADMINISTRADOR
                && membro.getPapel() != PapelOrganizacao.AUDITOR) {
            throw new AcessoNegadoException("Você não tem permissão para consultar membros desta organização");
        }
    }

    private Usuario validarPodeGerenciarMembros(UsuarioAutenticado autenticado, UUID organizacaoId) {
        Usuario usuario = buscarUsuarioAutenticado(autenticado);
        organizacaoService.buscarEntidadePorId(organizacaoId);

        if (isSuperAdministrador(usuario)) {
            return usuario;
        }

        OrganizacaoMembro membro = buscarVinculoAtivo(organizacaoId, usuario.getId());
        if (membro.getPapel() != PapelOrganizacao.PROPRIETARIO
                && membro.getPapel() != PapelOrganizacao.ADMINISTRADOR) {
            throw new AcessoNegadoException("Você não tem permissão para gerenciar membros desta organização");
        }

        return usuario;
    }

    private void validarPapelSensivel(
            Usuario executor,
            UUID organizacaoId,
            PapelOrganizacao novoPapel,
            OrganizacaoMembro membroAlvo
    ) {
        if (isSuperAdministrador(executor)) {
            return;
        }

        OrganizacaoMembro vinculoExecutor = buscarVinculoAtivo(organizacaoId, executor.getId());
        boolean executorEhProprietario = vinculoExecutor.getPapel() == PapelOrganizacao.PROPRIETARIO;
        boolean alvoEhProprietario = membroAlvo != null && membroAlvo.getPapel() == PapelOrganizacao.PROPRIETARIO;

        if (!executorEhProprietario && (novoPapel == PapelOrganizacao.PROPRIETARIO || alvoEhProprietario)) {
            throw new AcessoNegadoException("Somente o proprietário pode alterar vínculos de proprietário");
        }

        if (membroAlvo != null
                && alvoEhProprietario
                && novoPapel != PapelOrganizacao.PROPRIETARIO
                && !existeOutroProprietarioAtivo(organizacaoId, membroAlvo.getUsuario().getId())) {
            throw new ConflitoException("A organização precisa manter um proprietário ativo");
        }
    }

    private boolean existeOutroProprietarioAtivo(UUID organizacaoId, UUID usuarioIgnoradoId) {
        return membroRepository.findAllByOrganizacaoIdOrderByCriadoEmAsc(organizacaoId)
                .stream()
                .anyMatch(membro -> membro.getPapel() == PapelOrganizacao.PROPRIETARIO
                        && membro.getStatus() == StatusMembroOrganizacao.ATIVO
                        && !membro.getUsuario().getId().equals(usuarioIgnoradoId));
    }

    private OrganizacaoMembro buscarVinculoAtivo(UUID organizacaoId, UUID usuarioId) {
        OrganizacaoMembro membro = membroRepository.findByOrganizacaoIdAndUsuarioId(organizacaoId, usuarioId)
                .orElseThrow(() -> new AcessoNegadoException("Você não pertence a esta organização"));

        if (membro.getStatus() != StatusMembroOrganizacao.ATIVO) {
            throw new AcessoNegadoException("Seu vínculo com esta organização não está ativo");
        }

        return membro;
    }

    private OrganizacaoMembro novoMembro(
            Usuario usuario,
            Organizacao organizacao,
            PapelOrganizacao papel,
            StatusMembroOrganizacao status
    ) {
        OrganizacaoMembro membro = new OrganizacaoMembro();
        membro.setUsuario(usuario);
        membro.setOrganizacao(organizacao);
        membro.setPapel(papel);
        membro.setStatus(status);
        membro.setAceitoEm(LocalDateTime.now());
        return membro;
    }

    private void atualizarPapelCompatibilidade(OrganizacaoMembro membro, PerfilUsuario perfil) {
        PapelOrganizacao papel = resolverPapelCompatibilidade(perfil);
        if (membro.getPapel() != papel) {
            membro.setPapel(papel);
            membro.setPapelAlteradoEm(LocalDateTime.now());
        }

        if (membro.getStatus() == StatusMembroOrganizacao.REMOVIDO) {
            membro.setStatus(StatusMembroOrganizacao.ATIVO);
            membro.setRemovidoEm(null);
        }
    }

    private PapelOrganizacao resolverPapelCompatibilidade(PerfilUsuario perfil) {
        return switch (perfil) {
            case SUPER_ADMINISTRADOR, ADMINISTRADOR_ORGANIZACAO -> PapelOrganizacao.ADMINISTRADOR;
            case GESTOR -> PapelOrganizacao.GESTOR;
            case OPERADOR -> PapelOrganizacao.OPERADOR;
            case AUDITOR -> PapelOrganizacao.AUDITOR;
        };
    }

    private OrganizacaoMembro buscarMembroDaOrganizacao(UUID organizacaoId, UUID membroId) {
        OrganizacaoMembro membro = membroRepository.findById(membroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro da organização não encontrado"));

        if (!membro.getOrganizacao().getId().equals(organizacaoId)) {
            throw new RecursoNaoEncontradoException("Membro da organização não encontrado");
        }

        return membro;
    }

    private Usuario buscarUsuarioAutenticado(UsuarioAutenticado autenticado) {
        if (autenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }

        Usuario usuario = buscarUsuario(autenticado.id());
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        return usuario;
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private boolean isSuperAdministrador(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.SUPER_ADMINISTRADOR;
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
