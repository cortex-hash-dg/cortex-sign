package br.com.cortex.sign.modules.assinatura.controller;

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
import br.com.cortex.sign.modules.assinatura.service.AssinaturaService;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @PostMapping("/api/documentos/{documentoId}/solicitacoes-assinatura")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoAssinaturaResponse criarSolicitacao(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID documentoId,
            @Valid @RequestBody CriarSolicitacaoAssinaturaRequest request
    ) {
        return assinaturaService.criarSolicitacao(usuarioAutenticado, documentoId, request);
    }

    @GetMapping("/api/documentos/{documentoId}/solicitacoes-assinatura")
    public List<SolicitacaoAssinaturaResponse> listarPorDocumento(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID documentoId
    ) {
        return assinaturaService.listarPorDocumento(usuarioAutenticado, documentoId);
    }

    @PostMapping("/api/solicitacoes-assinatura/{id}/cancelar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        assinaturaService.cancelar(usuarioAutenticado, id);
    }

    @GetMapping("/api/publico/assinaturas/{token}")
    public AssinaturaPublicaResponse buscarPublica(@PathVariable String token) {
        return assinaturaService.buscarPublica(token);
    }

    @PostMapping("/api/publico/assinaturas/{token}/codigo")
    public CodigoAssinaturaSolicitadoResponse solicitarCodigo(
            @PathVariable String token,
            @Valid @RequestBody SolicitarCodigoAssinaturaRequest request
    ) {
        return assinaturaService.solicitarCodigo(token, request);
    }

    @PostMapping("/api/publico/assinaturas/{token}/acesso/solicitar")
    public CodigoAssinaturaSolicitadoResponse solicitarAcessoExterno(
            @PathVariable String token,
            @Valid @RequestBody SolicitarAcessoSignatarioExternoRequest request
    ) {
        return assinaturaService.solicitarAcessoExterno(token, request);
    }

    @PostMapping("/api/publico/assinaturas/{token}/acesso/validar")
    public AcessoSignatarioExternoResponse validarAcessoExterno(
            @PathVariable String token,
            @Valid @RequestBody ValidarAcessoSignatarioExternoRequest request
    ) {
        return assinaturaService.validarAcessoExterno(token, request);
    }

    @PostMapping("/api/publico/assinaturas/{token}/assinar")
    public SolicitacaoAssinaturaResponse assinarComToken(
            @PathVariable String token,
            @Valid @RequestBody AssinarComTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return assinaturaService.assinarComToken(token, request, servletRequest);
    }

    @PostMapping("/api/publico/assinaturas/{token}/rejeitar")
    public SolicitacaoAssinaturaResponse rejeitarComToken(
            @PathVariable String token,
            @Valid @RequestBody RejeitarAssinaturaRequest request,
            HttpServletRequest servletRequest
    ) {
        return assinaturaService.rejeitarComToken(token, request, servletRequest);
    }
}
