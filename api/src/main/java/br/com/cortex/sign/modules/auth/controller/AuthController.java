package br.com.cortex.sign.modules.auth.controller;

import br.com.cortex.sign.modules.auth.dto.request.AlterarSenhaRequest;
import br.com.cortex.sign.modules.auth.dto.request.AtualizarCadastroUsuarioRequest;
import br.com.cortex.sign.modules.auth.dto.request.AtualizarAssinaturaUsuarioRequest;
import br.com.cortex.sign.modules.auth.dto.request.ConfirmarEmailRequest;
import br.com.cortex.sign.modules.auth.dto.request.LoginRequest;
import br.com.cortex.sign.modules.auth.dto.request.LogoutRequest;
import br.com.cortex.sign.modules.auth.dto.request.RenovarTokenRequest;
import br.com.cortex.sign.modules.auth.dto.response.AssinaturaUsuarioResponse;
import br.com.cortex.sign.modules.auth.dto.response.CadastroUsuarioResponse;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmacaoEmailSolicitadaResponse;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmarEmailResponse;
import br.com.cortex.sign.modules.auth.dto.response.LoginResponse;
import br.com.cortex.sign.modules.auth.dto.response.UsuarioAutenticadoResponse;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.auth.service.AuthService;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoMembroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.login(request, servletRequest);
    }

    @PostMapping("/refresh")
    public LoginResponse renovarToken(
            @Valid @RequestBody RenovarTokenRequest request,
            HttpServletRequest servletRequest
    ) {
        return authService.renovarToken(request, servletRequest);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public UsuarioAutenticadoResponse me(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        return authService.me(usuarioAutenticado);
    }

    @GetMapping("/me/cadastro")
    public CadastroUsuarioResponse buscarCadastro(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado
    ) {
        return authService.buscarCadastro(usuarioAutenticado);
    }

    @PutMapping("/me/cadastro")
    public CadastroUsuarioResponse atualizarCadastro(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody AtualizarCadastroUsuarioRequest request
    ) {
        return authService.atualizarCadastro(usuarioAutenticado, request);
    }

    @PostMapping("/email/confirmacao")
    public ConfirmacaoEmailSolicitadaResponse solicitarConfirmacaoEmail(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado
    ) {
        return authService.solicitarConfirmacaoEmail(usuarioAutenticado);
    }

    @PostMapping("/email/confirmacao/validar")
    public ConfirmarEmailResponse confirmarEmail(@Valid @RequestBody ConfirmarEmailRequest request) {
        return authService.confirmarEmail(request);
    }

    @GetMapping("/me/organizacoes")
    public List<OrganizacaoMembroResponse> minhasOrganizacoes(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado
    ) {
        return authService.minhasOrganizacoes(usuarioAutenticado);
    }


    @GetMapping("/me/assinatura")
    public AssinaturaUsuarioResponse buscarAssinatura(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado
    ) {
        return authService.buscarAssinatura(usuarioAutenticado);
    }

    @PutMapping("/me/assinatura")
    public AssinaturaUsuarioResponse atualizarAssinatura(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody AtualizarAssinaturaUsuarioRequest request
    ) {
        return authService.atualizarAssinatura(usuarioAutenticado, request);
    }

    @PutMapping("/alterar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody AlterarSenhaRequest request
    ) {
        authService.alterarSenha(usuarioAutenticado, request);
    }
}
