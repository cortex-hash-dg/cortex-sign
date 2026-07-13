package br.com.cortex.sign.modules.signatario.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.auditoria.service.AuditoriaService;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.repository.DocumentoRepository;
import br.com.cortex.sign.modules.signatario.dto.request.AtualizarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.request.CriarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.response.SignatarioResponse;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import br.com.cortex.sign.modules.signatario.mapper.SignatarioMapper;
import br.com.cortex.sign.modules.signatario.repository.SignatarioRepository;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignatarioService {

    private final SignatarioRepository signatarioRepository;
    private final DocumentoRepository documentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SignatarioMapper signatarioMapper;
    private final AuditoriaService auditoriaService;

    @Transactional
    public SignatarioResponse criar(UsuarioAutenticado usuarioAutenticado, UUID documentoId, CriarSignatarioRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, documentoId);
        validarEmailDisponivel(documentoId, request.email(), null);

        Signatario signatario = signatarioRepository.saveAndFlush(signatarioMapper.toEntity(request, documento));
        auditoriaService.registrar(documento.getOrganizacao(), usuario, "SIGNATARIO_CRIADO", "SIGNATARIO", signatario.getId(), "Signatário criado para o documento " + documento.getTitulo());

        return signatarioMapper.toResponse(signatario);
    }

    @Transactional(readOnly = true)
    public List<SignatarioResponse> listarPorDocumento(UsuarioAutenticado usuarioAutenticado, UUID documentoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        buscarDocumentoComPermissao(usuario, documentoId);

        return signatarioRepository.findAllByDocumentoIdOrderByOrdemAssinaturaAscCriadoEmAsc(documentoId)
                .stream()
                .map(signatarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SignatarioResponse buscarPorId(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Signatario signatario = buscarSignatarioComPermissao(usuario, id);
        return signatarioMapper.toResponse(signatario);
    }

    @Transactional
    public SignatarioResponse atualizar(UsuarioAutenticado usuarioAutenticado, UUID id, AtualizarSignatarioRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Signatario signatario = buscarSignatarioComPermissao(usuario, id);
        validarEmailDisponivel(signatario.getDocumento().getId(), request.email(), id);

        signatarioMapper.updateEntity(signatario, request);
        Signatario atualizado = signatarioRepository.saveAndFlush(signatario);
        auditoriaService.registrar(signatario.getDocumento().getOrganizacao(), usuario, "SIGNATARIO_ATUALIZADO", "SIGNATARIO", id, "Signatário atualizado");

        return signatarioMapper.toResponse(atualizado);
    }

    @Transactional
    public void excluir(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Signatario signatario = buscarSignatarioComPermissao(usuario, id);

        signatarioRepository.delete(signatario);
        auditoriaService.registrar(signatario.getDocumento().getOrganizacao(), usuario, "SIGNATARIO_EXCLUIDO", "SIGNATARIO", id, "Signatário excluído");
    }

    private void validarEmailDisponivel(UUID documentoId, String email, UUID idIgnorado) {
        boolean existe = idIgnorado == null
                ? signatarioRepository.existsByDocumentoIdAndEmailIgnoreCase(documentoId, email.trim().toLowerCase())
                : signatarioRepository.existsByDocumentoIdAndEmailIgnoreCaseAndIdNot(documentoId, email.trim().toLowerCase(), idIgnorado);

        if (existe) {
            throw new ConflitoException("Já existe um signatário com este e-mail neste documento");
        }
    }

    private Signatario buscarSignatarioComPermissao(Usuario usuario, UUID id) {
        Signatario signatario = signatarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Signatário não encontrado"));

        validarPermissaoDocumento(usuario, signatario.getDocumento());
        return signatario;
    }

    private Documento buscarDocumentoComPermissao(Usuario usuario, UUID documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));

        validarPermissaoDocumento(usuario, documento);
        return documento;
    }

    private void validarPermissaoDocumento(Usuario usuario, Documento documento) {
        if (usuario.getPerfil() == PerfilUsuario.SUPER_ADMINISTRADOR) {
            return;
        }

        if (usuario.getOrganizacao() == null || !usuario.getOrganizacao().getId().equals(documento.getOrganizacao().getId())) {
            throw new AcessoNegadoException("Você não tem permissão para acessar este documento");
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
}
