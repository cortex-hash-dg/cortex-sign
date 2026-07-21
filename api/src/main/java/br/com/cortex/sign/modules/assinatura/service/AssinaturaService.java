package br.com.cortex.sign.modules.assinatura.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.integration.assinatura.ProvedorAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.DadosAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.ResultadoAssinatura;
import br.com.cortex.sign.modules.assinatura.dto.request.AssinarComTokenRequest;
import br.com.cortex.sign.modules.assinatura.dto.request.CriarSolicitacaoAssinaturaRequest;
import br.com.cortex.sign.modules.assinatura.dto.request.RejeitarAssinaturaRequest;
import br.com.cortex.sign.modules.assinatura.dto.response.AssinaturaPublicaResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.SolicitacaoAssinaturaResponse;
import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;
import br.com.cortex.sign.modules.assinatura.mapper.AssinaturaMapper;
import br.com.cortex.sign.modules.assinatura.repository.AssinaturaRepository;
import br.com.cortex.sign.modules.assinatura.repository.SolicitacaoAssinaturaRepository;
import br.com.cortex.sign.modules.auditoria.service.AuditoriaService;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.enums.StatusDocumento;
import br.com.cortex.sign.modules.documento.repository.DocumentoRepository;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import br.com.cortex.sign.modules.signatario.repository.SignatarioRepository;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SolicitacaoAssinaturaRepository solicitacaoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final DocumentoRepository documentoRepository;
    private final SignatarioRepository signatarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProvedorAssinatura provedorAssinatura;
    private final AssinaturaMapper assinaturaMapper;
    private final AuditoriaService auditoriaService;

    @Value("${app.assinatura.solicitacao.validade-dias:7}")
    private int validadePadraoDias;

    @Transactional
    public SolicitacaoAssinaturaResponse criarSolicitacao(
            UsuarioAutenticado usuarioAutenticado,
            UUID documentoId,
            CriarSolicitacaoAssinaturaRequest request
    ) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Documento documento = buscarDocumentoComPermissao(usuario, documentoId);
        Signatario signatario = buscarSignatarioDoDocumento(documentoId, request.signatarioId());

        if (solicitacaoRepository.existsBySignatarioIdAndStatus(signatario.getId(), StatusSolicitacaoAssinatura.PENDENTE)) {
            throw new ConflitoException("Já existe uma solicitação pendente para este signatário");
        }

        String codigo = gerarCodigo();
        SolicitacaoAssinatura solicitacao = new SolicitacaoAssinatura();
        solicitacao.setDocumento(documento);
        solicitacao.setSignatario(signatario);
        solicitacao.setStatus(StatusSolicitacaoAssinatura.PENDENTE);
        solicitacao.setToken(gerarToken());
        solicitacao.setCodigoHash(gerarHash(codigo));
        solicitacao.setExpiraEm(LocalDateTime.now().plusDays(resolverValidade(request.validadeDias())));

        SolicitacaoAssinatura salva = solicitacaoRepository.saveAndFlush(solicitacao);
        documento.setStatus(StatusDocumento.ENVIADO_PARA_ASSINATURA);
        documentoRepository.save(documento);

        auditoriaService.registrar(documento.getOrganizacao(), usuario, "SOLICITACAO_ASSINATURA_CRIADA", "SOLICITACAO_ASSINATURA", salva.getId(), "Solicitação criada para " + signatario.getEmail());

        return assinaturaMapper.toResponse(salva, null, codigo);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAssinaturaResponse> listarPorDocumento(UsuarioAutenticado usuarioAutenticado, UUID documentoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        buscarDocumentoComPermissao(usuario, documentoId);

        return solicitacaoRepository.findAllByDocumentoIdOrderByCriadoEmDesc(documentoId)
                .stream()
                .map(solicitacao -> assinaturaMapper.toResponse(solicitacao, buscarAssinaturaOpcional(solicitacao), null))
                .toList();
    }

    @Transactional
    public void cancelar(UsuarioAutenticado usuarioAutenticado, UUID solicitacaoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        SolicitacaoAssinatura solicitacao = buscarSolicitacao(solicitacaoId);
        validarPermissaoDocumento(usuario, solicitacao.getDocumento());

        if (solicitacao.getStatus() != StatusSolicitacaoAssinatura.PENDENTE) {
            throw new ConflitoException("Somente solicitações pendentes podem ser canceladas");
        }

        solicitacao.setStatus(StatusSolicitacaoAssinatura.CANCELADA);
        solicitacaoRepository.save(solicitacao);
        auditoriaService.registrar(solicitacao.getDocumento().getOrganizacao(), usuario, "SOLICITACAO_ASSINATURA_CANCELADA", "SOLICITACAO_ASSINATURA", solicitacao.getId(), "Solicitação cancelada");
    }

    @Transactional
    public AssinaturaPublicaResponse buscarPublica(String token) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        atualizarStatusSeExpirada(solicitacao);
        return assinaturaMapper.toPublicResponse(solicitacao);
    }

    @Transactional
    public SolicitacaoAssinaturaResponse assinarComToken(String token, AssinarComTokenRequest request, HttpServletRequest servletRequest) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarCodigo(solicitacao, request.codigo());

        TipoAssinatura tipo = resolverTipoAssinatura(request.tipo());
        Documento documento = solicitacao.getDocumento();
        String documentoHash = obterHashDocumento(documento);
        ResultadoAssinatura resultado = provedorAssinatura.assinar(new DadosAssinatura(
                documento.getId(),
                documento.getTitulo(),
                documentoHash,
                documento.getArquivoAtual().getTamanhoBytes(),
                documento.getOrganizacao().getId(),
                documento.getOrganizacao().getNome(),
                solicitacao.getId(),
                solicitacao.getCriadoEm(),
                solicitacao.getExpiraEm(),
                solicitacao.getSignatario().getId(),
                solicitacao.getSignatario().getNome(),
                solicitacao.getSignatario().getEmail(),
                solicitacao.getCodigoHash(),
                obterIp(servletRequest),
                obterUserAgent(servletRequest)
        ));

        Assinatura assinatura = new Assinatura();
        assinatura.setSolicitacaoAssinatura(solicitacao);
        assinatura.setTipo(tipo);
        assinatura.setStatus(StatusAssinatura.VALIDA);
        assinatura.setProvedor(resultado.provedor());
        assinatura.setProtocolo(resultado.protocolo());
        assinatura.setAssinadoEm(resultado.assinadoEm());
        assinatura.setDocumentoHashSha256(documentoHash);
        assinatura.setEvidenciaHashSha256(resultado.evidenciaHashSha256());
        assinatura.setTermoAceite(resultado.termoAceite());
        assinatura.setIpAssinatura(obterIp(servletRequest));
        assinatura.setUserAgent(limitar(obterUserAgent(servletRequest), 500));
        assinatura.setMetadados(resultado.metadados());

        Assinatura assinaturaSalva = assinaturaRepository.saveAndFlush(assinatura);
        solicitacao.setStatus(StatusSolicitacaoAssinatura.ASSINADA);
        solicitacaoRepository.saveAndFlush(solicitacao);

        atualizarDocumentoAposAssinatura(solicitacao.getDocumento());
        auditoriaService.registrar(solicitacao.getDocumento().getOrganizacao(), null, "DOCUMENTO_ASSINADO", "ASSINATURA", assinaturaSalva.getId(), "Documento assinado por " + solicitacao.getSignatario().getEmail());

        return assinaturaMapper.toResponse(solicitacao, assinaturaSalva, null);
    }

    @Transactional
    public SolicitacaoAssinaturaResponse rejeitarComToken(String token, RejeitarAssinaturaRequest request, HttpServletRequest servletRequest) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarCodigo(solicitacao, request.codigo());

        Assinatura assinatura = new Assinatura();
        assinatura.setSolicitacaoAssinatura(solicitacao);
        assinatura.setTipo(TipoAssinatura.TOKEN_CODIGO);
        assinatura.setStatus(StatusAssinatura.REJEITADA);
        assinatura.setProvedor(provedorAssinatura.getProviderType());
        assinatura.setRejeitadoEm(LocalDateTime.now());
        assinatura.setMotivoRejeicao(request.motivo().trim());
        assinatura.setDocumentoHashSha256(obterHashDocumento(solicitacao.getDocumento()));
        assinatura.setIpAssinatura(obterIp(servletRequest));
        assinatura.setUserAgent(limitar(obterUserAgent(servletRequest), 500));
        assinatura.setMetadados("Assinatura rejeitada pelo signatário");

        Assinatura assinaturaSalva = assinaturaRepository.saveAndFlush(assinatura);
        solicitacao.setStatus(StatusSolicitacaoAssinatura.REJEITADA);
        solicitacaoRepository.saveAndFlush(solicitacao);
        solicitacao.getDocumento().setStatus(StatusDocumento.REJEITADO);
        documentoRepository.save(solicitacao.getDocumento());

        auditoriaService.registrar(solicitacao.getDocumento().getOrganizacao(), null, "ASSINATURA_REJEITADA", "ASSINATURA", assinaturaSalva.getId(), "Assinatura rejeitada por " + solicitacao.getSignatario().getEmail());

        return assinaturaMapper.toResponse(solicitacao, assinaturaSalva, null);
    }



    private String obterHashDocumento(Documento documento) {
        if (documento.getArquivoAtual() == null || documento.getArquivoAtual().getChecksumSha256() == null) {
            throw new ConflitoException("Documento sem hash SHA-256 registrado. Faça novo upload antes de assinar");
        }

        return documento.getArquivoAtual().getChecksumSha256();
    }

    private TipoAssinatura resolverTipoAssinatura(TipoAssinatura tipoInformado) {
        if (tipoInformado == null) {
            return TipoAssinatura.TOKEN_CODIGO;
        }

        if (tipoInformado == TipoAssinatura.ICP_BRASIL) {
            throw new ConflitoException("Assinatura ICP-Brasil ainda não está disponível neste fluxo");
        }

        return tipoInformado;
    }

    private void atualizarDocumentoAposAssinatura(Documento documento) {
        long pendentes = solicitacaoRepository.countByDocumentoIdAndStatus(documento.getId(), StatusSolicitacaoAssinatura.PENDENTE);
        if (pendentes == 0) {
            documento.setStatus(StatusDocumento.ASSINADO);
            documentoRepository.save(documento);
        }
    }

    private void validarSolicitacaoPodeSerUsada(SolicitacaoAssinatura solicitacao) {
        atualizarStatusSeExpirada(solicitacao);

        if (solicitacao.getStatus() != StatusSolicitacaoAssinatura.PENDENTE) {
            throw new ConflitoException("Esta solicitação não está pendente");
        }
    }

    private void atualizarStatusSeExpirada(SolicitacaoAssinatura solicitacao) {
        if (solicitacao.getStatus() == StatusSolicitacaoAssinatura.PENDENTE
                && LocalDateTime.now().isAfter(solicitacao.getExpiraEm())) {
            solicitacao.setStatus(StatusSolicitacaoAssinatura.EXPIRADA);
            solicitacaoRepository.save(solicitacao);
        }
    }

    private void validarCodigo(SolicitacaoAssinatura solicitacao, String codigo) {
        if (!solicitacao.getCodigoHash().equals(gerarHash(codigo))) {
            throw new CredenciaisInvalidasException("Código de assinatura inválido");
        }
    }

    private Assinatura buscarAssinaturaOpcional(SolicitacaoAssinatura solicitacao) {
        return assinaturaRepository.findBySolicitacaoAssinaturaId(solicitacao.getId()).orElse(null);
    }

    private SolicitacaoAssinatura buscarSolicitacao(UUID id) {
        return solicitacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de assinatura não encontrada"));
    }

    private SolicitacaoAssinatura buscarSolicitacaoPorToken(String token) {
        return solicitacaoRepository.findByToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de assinatura não encontrada"));
    }

    private Signatario buscarSignatarioDoDocumento(UUID documentoId, UUID signatarioId) {
        Signatario signatario = signatarioRepository.findById(signatarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Signatário não encontrado"));

        if (!signatario.getDocumento().getId().equals(documentoId)) {
            throw new ConflitoException("Signatário não pertence ao documento informado");
        }

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

    private int resolverValidade(Integer validadeDias) {
        return validadeDias != null ? validadeDias : validadePadraoDias;
    }

    private String gerarToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String gerarCodigo() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }

    private String gerarHash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o código de assinatura", exception);
        }
    }

    private String obterIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return limitar(forwardedFor.split(",")[0].trim(), 80);
        }

        return limitar(request.getRemoteAddr(), 80);
    }

    private String obterUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String limitar(String valor, int limite) {
        if (valor == null) {
            return null;
        }

        return valor.length() <= limite ? valor : valor.substring(0, limite);
    }
}
