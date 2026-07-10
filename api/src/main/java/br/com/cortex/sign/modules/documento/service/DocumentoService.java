package br.com.cortex.sign.modules.documento.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.integration.storage.StorageProvider;
import br.com.cortex.sign.integration.storage.dto.ArquivoUploadResultado;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.documento.dto.request.AtualizarDocumentoRequest;
import br.com.cortex.sign.modules.documento.dto.request.CriarDocumentoRequest;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoDetalheResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoDownloadResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoResumoResponse;
import br.com.cortex.sign.modules.documento.entity.ArquivoArmazenado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.entity.DocumentoVersao;
import br.com.cortex.sign.modules.documento.enums.StatusDocumento;
import br.com.cortex.sign.modules.documento.mapper.DocumentoMapper;
import br.com.cortex.sign.modules.documento.repository.ArquivoArmazenadoRepository;
import br.com.cortex.sign.modules.documento.repository.DocumentoRepository;
import br.com.cortex.sign.modules.documento.repository.DocumentoVersaoRepository;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoService;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final DocumentoRepository documentoRepository;
    private final DocumentoVersaoRepository documentoVersaoRepository;
    private final ArquivoArmazenadoRepository arquivoArmazenadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoService organizacaoService;
    private final StorageProvider storageProvider;
    private final DocumentoMapper documentoMapper;

    @Value("${app.documento.upload.max-size-mb:25}")
    private long maxSizeMb;

    @Transactional
    public DocumentoDetalheResponse criar(UsuarioAutenticado usuarioAutenticado, CriarDocumentoRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Organizacao organizacao = resolverOrganizacaoParaCriacao(usuario, request.organizacaoId());
        MultipartFile arquivo = request.arquivo();

        validarArquivoPdf(arquivo);

        ArquivoUploadResultado upload = storageProvider.upload(
                arquivo,
                "organizacoes/" + organizacao.getId() + "/documentos"
        );

        ArquivoArmazenado arquivoArmazenado = salvarArquivo(upload);

        Documento documento = new Documento();
        documento.setOrganizacao(organizacao);
        documento.setArquivoAtual(arquivoArmazenado);
        documento.setTitulo(resolverTitulo(request.titulo(), upload.nomeOriginal()));
        documento.setStatus(StatusDocumento.RASCUNHO);
        documento.setCriadoPorUsuario(usuario);

        Documento documentoSalvo = documentoRepository.saveAndFlush(documento);

        DocumentoVersao versao = new DocumentoVersao();
        versao.setDocumento(documentoSalvo);
        versao.setArquivo(arquivoArmazenado);
        versao.setNumeroVersao(1);
        versao.setCriadoPorUsuario(usuario);
        documentoVersaoRepository.saveAndFlush(versao);

        return buscarPorId(usuarioAutenticado, documentoSalvo.getId());
    }

    @Transactional(readOnly = true)
    public List<DocumentoResumoResponse> listar(UsuarioAutenticado usuarioAutenticado, UUID organizacaoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);

        if (isSuperAdministrador(usuario)) {
            if (organizacaoId != null) {
                organizacaoService.buscarEntidadePorId(organizacaoId);
                return documentoRepository.findAllByOrganizacaoIdOrderByCriadoEmDesc(organizacaoId)
                        .stream()
                        .map(documentoMapper::toResumoResponse)
                        .toList();
            }

            return documentoRepository.findAllByOrderByCriadoEmDesc()
                    .stream()
                    .map(documentoMapper::toResumoResponse)
                    .toList();
        }

        Organizacao organizacao = resolverOrganizacaoDoUsuario(usuario);
        validarOrganizacaoSolicitada(usuario, organizacaoId);

        return documentoRepository.findAllByOrganizacaoIdOrderByCriadoEmDesc(organizacao.getId())
                .stream()
                .map(documentoMapper::toResumoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentoDetalheResponse buscarPorId(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, id);
        List<DocumentoVersao> versoes = documentoVersaoRepository.findAllByDocumentoIdOrderByNumeroVersaoAsc(id);

        return documentoMapper.toDetalheResponse(documento, versoes);
    }

    @Transactional
    public DocumentoDetalheResponse atualizar(UsuarioAutenticado usuarioAutenticado, UUID id, AtualizarDocumentoRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, id);

        documento.setTitulo(request.titulo().trim());
        documentoRepository.saveAndFlush(documento);

        return buscarPorId(usuarioAutenticado, id);
    }

    @Transactional
    public void arquivar(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, id);

        documento.setStatus(StatusDocumento.ARQUIVADO);
        documentoRepository.save(documento);
    }

    @Transactional(readOnly = true)
    public DocumentoDownloadResponse baixar(UsuarioAutenticado usuarioAutenticado, UUID id) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, id);
        ArquivoArmazenado arquivo = documento.getArquivoAtual();
        Resource resource = storageProvider.download(arquivo.getCaminho());

        return new DocumentoDownloadResponse(
                resource,
                arquivo.getNomeOriginal(),
                arquivo.getTipoConteudo(),
                arquivo.getTamanhoBytes()
        );
    }

    private ArquivoArmazenado salvarArquivo(ArquivoUploadResultado upload) {
        ArquivoArmazenado arquivo = new ArquivoArmazenado();
        arquivo.setProvedor(upload.provedor());
        arquivo.setNomeOriginal(upload.nomeOriginal());
        arquivo.setNomeArmazenado(upload.nomeArmazenado());
        arquivo.setCaminho(upload.caminho());
        arquivo.setTipoConteudo(upload.tipoConteudo());
        arquivo.setTamanhoBytes(upload.tamanhoBytes());
        arquivo.setChecksumSha256(upload.checksumSha256());

        return arquivoArmazenadoRepository.saveAndFlush(arquivo);
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

    private Organizacao resolverOrganizacaoParaCriacao(Usuario usuario, UUID organizacaoId) {
        if (isSuperAdministrador(usuario)) {
            if (organizacaoId == null) {
                throw new ConflitoException("A organização é obrigatória para criar documento como Super Admin");
            }

            return organizacaoService.buscarEntidadePorId(organizacaoId);
        }

        Organizacao organizacao = resolverOrganizacaoDoUsuario(usuario);
        validarOrganizacaoSolicitada(usuario, organizacaoId);
        return organizacao;
    }

    private Documento buscarDocumentoComPermissao(Usuario usuario, UUID documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento não encontrado"));

        if (!isSuperAdministrador(usuario)
                && !documento.getOrganizacao().getId().equals(resolverOrganizacaoDoUsuario(usuario).getId())) {
            throw new AcessoNegadoException("Você não tem permissão para acessar este documento");
        }

        return documento;
    }

    private Organizacao resolverOrganizacaoDoUsuario(Usuario usuario) {
        if (usuario.getOrganizacao() == null) {
            throw new ConflitoException("Usuário não está vinculado a uma organização");
        }

        return usuario.getOrganizacao();
    }

    private void validarOrganizacaoSolicitada(Usuario usuario, UUID organizacaoId) {
        if (organizacaoId == null || isSuperAdministrador(usuario)) {
            return;
        }

        UUID organizacaoDoUsuario = resolverOrganizacaoDoUsuario(usuario).getId();
        if (!organizacaoDoUsuario.equals(organizacaoId)) {
            throw new AcessoNegadoException("Você não tem permissão para acessar documentos de outra organização");
        }
    }

    private void validarArquivoPdf(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ConflitoException("O arquivo PDF é obrigatório");
        }

        long maxBytes = maxSizeMb * 1024 * 1024;
        if (arquivo.getSize() > maxBytes) {
            throw new ConflitoException("O arquivo PDF deve ter no máximo " + maxSizeMb + " MB");
        }

        String nomeOriginal = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase();
        String tipoConteudo = arquivo.getContentType();
        boolean contentTypePdf = PDF_CONTENT_TYPE.equalsIgnoreCase(tipoConteudo);
        boolean nomePdf = nomeOriginal.endsWith(".pdf");

        if (!contentTypePdf && !nomePdf) {
            throw new ConflitoException("Somente arquivos PDF são permitidos");
        }
    }

    private String resolverTitulo(String titulo, String nomeOriginal) {
        if (titulo != null && !titulo.isBlank()) {
            return limitarTitulo(titulo.trim());
        }

        String nome = nomeOriginal == null || nomeOriginal.isBlank() ? "Documento" : nomeOriginal;
        int index = nome.lastIndexOf('.');
        String tituloSemExtensao = index > 0 ? nome.substring(0, index) : nome;
        return limitarTitulo(tituloSemExtensao);
    }

    private String limitarTitulo(String titulo) {
        return titulo.length() <= 200 ? titulo : titulo.substring(0, 200);
    }

    private boolean isSuperAdministrador(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.SUPER_ADMINISTRADOR;
    }
}
