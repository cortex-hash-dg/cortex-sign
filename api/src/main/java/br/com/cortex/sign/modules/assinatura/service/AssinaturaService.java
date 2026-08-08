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
import br.com.cortex.sign.modules.assinatura.dto.request.SolicitarAcessoSignatarioExternoRequest;
import br.com.cortex.sign.modules.assinatura.dto.request.SolicitarCodigoAssinaturaRequest;
import br.com.cortex.sign.modules.assinatura.dto.request.ValidarAcessoSignatarioExternoRequest;
import br.com.cortex.sign.modules.assinatura.dto.response.AcessoSignatarioExternoResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.AssinaturaPublicaResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.CodigoAssinaturaSolicitadoResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.SolicitacaoAssinaturaResponse;
import br.com.cortex.sign.modules.assinatura.entity.AcessoSignatarioExterno;
import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;
import br.com.cortex.sign.modules.assinatura.mapper.AssinaturaMapper;
import br.com.cortex.sign.modules.assinatura.repository.AcessoSignatarioExternoRepository;
import br.com.cortex.sign.modules.assinatura.repository.AssinaturaRepository;
import br.com.cortex.sign.modules.assinatura.repository.SolicitacaoAssinaturaRepository;
import br.com.cortex.sign.modules.auditoria.service.AuditoriaService;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.enums.StatusDocumento;
import br.com.cortex.sign.modules.documento.repository.DocumentoRepository;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import br.com.cortex.sign.modules.signatario.enums.TipoSignatario;
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
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SolicitacaoAssinaturaRepository solicitacaoRepository;
    private final AcessoSignatarioExternoRepository acessoExternoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final DocumentoRepository documentoRepository;
    private final SignatarioRepository signatarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProvedorAssinatura provedorAssinatura;
    private final AssinaturaMapper assinaturaMapper;
    private final AuditoriaService auditoriaService;
    private final DocumentoAssinadoService documentoAssinadoService;
    private final CodigoAssinaturaEntregaService codigoEntregaService;

    @Value("${app.assinatura.solicitacao.validade-dias:7}")
    private int validadePadraoDias;

    @Value("${app.assinatura.acesso-externo.codigo-expiracao-minutos:15}")
    private int codigoAcessoExternoExpiracaoMinutos;

    @Value("${app.assinatura.acesso-externo.token-expiracao-dias:30}")
    private int tokenAcessoExternoExpiracaoDias;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

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

        SolicitacaoAssinatura solicitacao = new SolicitacaoAssinatura();
        String token = gerarToken();
        solicitacao.setDocumento(documento);
        solicitacao.setSignatario(signatario);
        solicitacao.setStatus(StatusSolicitacaoAssinatura.PENDENTE);
        solicitacao.setToken(token);
        solicitacao.setCodigoHash(gerarHash(token));
        solicitacao.setExpiraEm(resolverExpiracao(request));

        SolicitacaoAssinatura salva = solicitacaoRepository.saveAndFlush(solicitacao);
        documento.setStatus(StatusDocumento.ENVIADO_PARA_ASSINATURA);
        documentoRepository.save(documento);

        auditoriaService.registrar(documento.getOrganizacao(), usuario, "SOLICITACAO_ASSINATURA_CRIADA", "SOLICITACAO_ASSINATURA", salva.getId(), "Solicitação criada para " + signatario.getEmail());

        return assinaturaMapper.toResponse(salva, null);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAssinaturaResponse> listarPorDocumento(UsuarioAutenticado usuarioAutenticado, UUID documentoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        buscarDocumentoComPermissao(usuario, documentoId);

        return solicitacaoRepository.findAllByDocumentoIdOrderByCriadoEmDesc(documentoId)
                .stream()
                .map(solicitacao -> assinaturaMapper.toResponse(solicitacao, buscarAssinaturaOpcional(solicitacao)))
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
    public CodigoAssinaturaSolicitadoResponse solicitarCodigo(String token, SolicitarCodigoAssinaturaRequest request) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);

        codigoEntregaService.enviarLink(solicitacao, request.canal(), criarLinkAssinatura(solicitacao.getToken()));

        auditoriaService.registrar(
                solicitacao.getDocumento().getOrganizacao(),
                null,
                "LINK_ASSINATURA_ENVIADO",
                "SOLICITACAO_ASSINATURA",
                solicitacao.getId(),
                "Link de assinatura enviado por " + request.canal() + " para " + solicitacao.getSignatario().getEmail()
        );

        return new CodigoAssinaturaSolicitadoResponse(
                request.canal(),
                mascararDestino(solicitacao.getSignatario(), request.canal()),
                "Link de assinatura enviado pelo canal solicitado",
                LocalDateTime.now()
        );
    }

    @Transactional
    public CodigoAssinaturaSolicitadoResponse solicitarAcessoExterno(String token, SolicitarAcessoSignatarioExternoRequest request) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarSignatarioExterno(solicitacao);

        String cpfNormalizado = validarCpfDoSignatario(solicitacao.getSignatario(), request.cpf());
        String codigo = gerarCodigoNumerico();

        AcessoSignatarioExterno acesso = new AcessoSignatarioExterno();
        acesso.setSolicitacaoAssinatura(solicitacao);
        acesso.setCpfHash(gerarHash(cpfNormalizado));
        acesso.setCodigoHash(gerarHash(codigo));
        acesso.setCanal(request.canal());
        acesso.setDestinoMascarado(mascararDestino(solicitacao.getSignatario(), request.canal()));
        acesso.setCodigoExpiraEm(LocalDateTime.now().plusMinutes(codigoAcessoExternoExpiracaoMinutos));
        acessoExternoRepository.save(acesso);

        codigoEntregaService.enviarCodigoAcessoExterno(solicitacao, request.canal(), codigo);

        auditoriaService.registrar(
                solicitacao.getDocumento().getOrganizacao(),
                null,
                "ACESSO_SIGNATARIO_EXTERNO_SOLICITADO",
                "SOLICITACAO_ASSINATURA",
                solicitacao.getId(),
                "Código de acesso externo enviado por " + request.canal() + " para " + acesso.getDestinoMascarado()
        );

        return new CodigoAssinaturaSolicitadoResponse(
                request.canal(),
                acesso.getDestinoMascarado(),
                "Código de acesso enviado pelo canal solicitado",
                LocalDateTime.now()
        );
    }

    @Transactional
    public AcessoSignatarioExternoResponse validarAcessoExterno(String token, ValidarAcessoSignatarioExternoRequest request) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarSignatarioExterno(solicitacao);

        String cpfNormalizado = validarCpfDoSignatario(solicitacao.getSignatario(), request.cpf());
        String cpfHash = gerarHash(cpfNormalizado);

        AcessoSignatarioExterno acesso = acessoExternoRepository
                .findTopBySolicitacaoAssinaturaIdAndCpfHashOrderByCriadoEmDesc(solicitacao.getId(), cpfHash)
                .orElseThrow(() -> new CredenciaisInvalidasException("Solicite um código de acesso antes de continuar"));

        if (acesso.getCodigoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new CredenciaisInvalidasException("Código de acesso expirado");
        }

        if (!acesso.getCodigoHash().equals(gerarHash(request.codigo()))) {
            throw new CredenciaisInvalidasException("Código de acesso inválido");
        }

        String tokenAcesso = gerarToken();
        LocalDateTime tokenExpiraEm = LocalDateTime.now().plusDays(tokenAcessoExternoExpiracaoDias);
        acesso.setTokenAcessoHash(gerarHash(tokenAcesso));
        acesso.setTokenExpiraEm(tokenExpiraEm);
        acesso.setValidadoEm(LocalDateTime.now());
        acessoExternoRepository.save(acesso);

        auditoriaService.registrar(
                solicitacao.getDocumento().getOrganizacao(),
                null,
                "ACESSO_SIGNATARIO_EXTERNO_VALIDADO",
                "SOLICITACAO_ASSINATURA",
                solicitacao.getId(),
                "Acesso externo validado para " + solicitacao.getSignatario().getEmail()
        );

        return new AcessoSignatarioExternoResponse(
                tokenAcesso,
                tokenExpiraEm,
                "Acesso liberado"
        );
    }

    @Transactional
    public SolicitacaoAssinaturaResponse assinarComToken(String token, AssinarComTokenRequest request, HttpServletRequest servletRequest) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarAcessoExternoSeNecessario(solicitacao, request.tokenAcessoExterno());
        validarCodigoSeInformado(solicitacao, request.codigo());

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

        String assinaturaManuscritaBase64 = normalizarAssinaturaManuscrita(request.assinaturaManuscritaBase64());

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
        assinatura.setMetadados(montarMetadados(resultado.metadados(), assinaturaManuscritaBase64));

        Assinatura assinaturaSalva = assinaturaRepository.saveAndFlush(assinatura);
        solicitacao.setStatus(StatusSolicitacaoAssinatura.ASSINADA);
        solicitacaoRepository.saveAndFlush(solicitacao);

        documentoAssinadoService.registrarAssinaturaVisual(solicitacao.getDocumento(), assinaturaSalva);
        atualizarDocumentoAposAssinatura(solicitacao.getDocumento());
        auditoriaService.registrar(solicitacao.getDocumento().getOrganizacao(), null, "DOCUMENTO_ASSINADO", "ASSINATURA", assinaturaSalva.getId(), "Documento assinado por " + solicitacao.getSignatario().getEmail());

        return assinaturaMapper.toResponse(solicitacao, assinaturaSalva);
    }

    @Transactional
    public SolicitacaoAssinaturaResponse rejeitarComToken(String token, RejeitarAssinaturaRequest request, HttpServletRequest servletRequest) {
        SolicitacaoAssinatura solicitacao = buscarSolicitacaoPorToken(token);
        validarSolicitacaoPodeSerUsada(solicitacao);
        validarAcessoExternoSeNecessario(solicitacao, request.tokenAcessoExterno());
        validarCodigoSeInformado(solicitacao, request.codigo());

        Assinatura assinatura = new Assinatura();
        assinatura.setSolicitacaoAssinatura(solicitacao);
        assinatura.setTipo(TipoAssinatura.LINK_TOKEN);
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

        return assinaturaMapper.toResponse(solicitacao, assinaturaSalva);
    }



    private String obterHashDocumento(Documento documento) {
        if (documento.getArquivoAtual() == null || documento.getArquivoAtual().getChecksumSha256() == null) {
            throw new ConflitoException("Documento sem hash SHA-256 registrado. Faça novo upload antes de assinar");
        }

        return documento.getArquivoAtual().getChecksumSha256();
    }

    private String normalizarAssinaturaManuscrita(String assinaturaManuscritaBase64) {
        if (assinaturaManuscritaBase64 == null || assinaturaManuscritaBase64.isBlank()) {
            return null;
        }

        String valor = assinaturaManuscritaBase64.trim();
        if (valor.startsWith("data:image/")) {
            return valor;
        }

        return "data:image/png;base64," + valor;
    }

    private String montarMetadados(String metadadosOriginais, String assinaturaManuscritaBase64) {
        if (assinaturaManuscritaBase64 == null) {
            return metadadosOriginais;
        }

        String metadados = metadadosOriginais == null || metadadosOriginais.isBlank() ? "" : metadadosOriginais.trim() + "\n";
        return metadados
                + "assinaturaManuscritaHashSha256=" + gerarHash(assinaturaManuscritaBase64) + "\n"
                + "assinaturaManuscritaBase64=" + assinaturaManuscritaBase64;
    }

    private TipoAssinatura resolverTipoAssinatura(TipoAssinatura tipoInformado) {
        if (tipoInformado == null) {
            return TipoAssinatura.LINK_TOKEN;
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
                && solicitacao.getExpiraEm() != null
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

    private void validarCodigoSeInformado(SolicitacaoAssinatura solicitacao, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return;
        }

        validarCodigo(solicitacao, codigo);
    }

    private void validarAcessoExternoSeNecessario(SolicitacaoAssinatura solicitacao, String tokenAcessoExterno) {
        if (solicitacao.getSignatario().getTipo() != TipoSignatario.EXTERNO) {
            return;
        }

        if (tokenAcessoExterno == null || tokenAcessoExterno.isBlank()) {
            throw new CredenciaisInvalidasException("Libere o acesso externo antes de assinar este documento");
        }

        acessoExternoRepository
                .findBySolicitacaoAssinaturaIdAndTokenAcessoHashAndTokenExpiraEmAfter(
                        solicitacao.getId(),
                        gerarHash(tokenAcessoExterno),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new CredenciaisInvalidasException("Acesso externo inválido ou expirado"));
    }

    private void validarSignatarioExterno(SolicitacaoAssinatura solicitacao) {
        if (solicitacao.getSignatario().getTipo() != TipoSignatario.EXTERNO) {
            throw new ConflitoException("Esta solicitação não pertence a um signatário externo");
        }
    }

    private String validarCpfDoSignatario(Signatario signatario, String cpfInformado) {
        String documentoCadastrado = normalizarDocumento(signatario.getNumeroDocumento());
        if (documentoCadastrado == null) {
            throw new ConflitoException("Signatário externo sem CPF cadastrado");
        }

        String cpfNormalizado = normalizarDocumento(cpfInformado);
        if (cpfNormalizado == null || !documentoCadastrado.equals(cpfNormalizado)) {
            throw new CredenciaisInvalidasException("CPF informado não confere com o signatário");
        }

        return cpfNormalizado;
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

    private LocalDateTime resolverExpiracao(CriarSolicitacaoAssinaturaRequest request) {
        if (Boolean.TRUE.equals(request.semValidade())) {
            return null;
        }

        return LocalDateTime.now().plusDays(resolverValidade(request.validadeDias()));
    }

    private int resolverValidade(Integer validadeDias) {
        return validadeDias != null ? validadeDias : validadePadraoDias;
    }

    private String gerarToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String gerarCodigoNumerico() {
        return String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
    }

    private String gerarHash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger a autenticação da assinatura", exception);
        }
    }

    private String criarLinkAssinatura(String token) {
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl.replaceAll("/+$", ""))
                .path("/assinaturas/")
                .path(token)
                .build()
                .toUriString();
    }

    private String mascararDestino(Signatario signatario, CanalCodigoAssinatura canal) {
        return mascararEmail(signatario.getEmail());
    }

    private String normalizarDocumento(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String digitos = valor.replaceAll("\\D", "");
        return digitos.isBlank() ? null : digitos;
    }

    private String mascararEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "e-mail cadastrado";
        }

        String[] partes = email.split("@", 2);
        String nome = partes[0];
        String inicio = nome.length() <= 2 ? nome.substring(0, 1) : nome.substring(0, 2);
        return inicio + "***@" + partes[1];
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
