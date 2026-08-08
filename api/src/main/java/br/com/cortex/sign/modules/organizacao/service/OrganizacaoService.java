package br.com.cortex.sign.modules.organizacao.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.CriarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.mapper.OrganizacaoMapper;
import br.com.cortex.sign.modules.organizacao.repository.OrganizacaoRepository;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final OrganizacaoMapper organizacaoMapper;

    @Transactional
    public OrganizacaoResponse criar(UsuarioAutenticado usuarioAutenticado, CriarOrganizacaoRequest request) {
        validarSuperAdmin(usuarioAutenticado);

        Organizacao organizacao = organizacaoMapper.toEntity(request);
        Organizacao organizacaoSalva = organizacaoRepository.saveAndFlush(organizacao);

        return organizacaoMapper.toResponse(organizacaoSalva);
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponse> listar(UsuarioAutenticado usuarioAutenticado) {
        validarAutenticado(usuarioAutenticado);

        if (!isSuperAdmin(usuarioAutenticado)) {
            return List.of(organizacaoMapper.toResponse(buscarOrganizacaoDoUsuario(usuarioAutenticado)));
        }

        return organizacaoRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Organizacao::getNome))
                .map(organizacaoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse buscarPorId(UsuarioAutenticado usuarioAutenticado, UUID id) {
        validarAcessoOrganizacao(usuarioAutenticado, id);

        return organizacaoMapper.toResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public OrganizacaoResponse atualizar(UsuarioAutenticado usuarioAutenticado, UUID id, AtualizarOrganizacaoRequest request) {
        validarAcessoOrganizacao(usuarioAutenticado, id);

        Organizacao organizacao = buscarEntidadePorId(id);

        organizacaoMapper.updateEntity(organizacao, request);

        Organizacao organizacaoAtualizada = organizacaoRepository.saveAndFlush(organizacao);

        return organizacaoMapper.toResponse(organizacaoAtualizada);
    }

    @Transactional
    public void excluir(UsuarioAutenticado usuarioAutenticado, UUID id) {
        validarSuperAdmin(usuarioAutenticado);

        Organizacao organizacao = buscarEntidadePorId(id);

        organizacaoRepository.delete(organizacao);
    }

    public Organizacao buscarEntidadePorId(UUID id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada"));
    }

    private void validarAcessoOrganizacao(UsuarioAutenticado usuarioAutenticado, UUID organizacaoId) {
        validarAutenticado(usuarioAutenticado);

        if (isSuperAdmin(usuarioAutenticado)) {
            return;
        }

        if (usuarioAutenticado.organizacaoId() == null || !usuarioAutenticado.organizacaoId().equals(organizacaoId)) {
            throw new AcessoNegadoException("Você não tem permissão para acessar esta organização");
        }
    }

    private Organizacao buscarOrganizacaoDoUsuario(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado.organizacaoId() == null) {
            throw new AcessoNegadoException("Usuário não está vinculado a uma organização");
        }

        return buscarEntidadePorId(usuarioAutenticado.organizacaoId());
    }

    private void validarSuperAdmin(UsuarioAutenticado usuarioAutenticado) {
        validarAutenticado(usuarioAutenticado);

        if (!isSuperAdmin(usuarioAutenticado)) {
            throw new AcessoNegadoException("Somente Super Admin pode gerenciar organizações");
        }
    }

    private void validarAutenticado(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }
    }

    private boolean isSuperAdmin(UsuarioAutenticado usuarioAutenticado) {
        return usuarioAutenticado.perfil() == PerfilUsuario.SUPER_ADMINISTRADOR;
    }
}
