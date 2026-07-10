package br.com.cortex.sign.modules.auth.service;

import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.modules.auth.dto.request.AlterarSenhaRequest;
import br.com.cortex.sign.modules.auth.dto.request.LoginRequest;
import br.com.cortex.sign.modules.auth.dto.request.LogoutRequest;
import br.com.cortex.sign.modules.auth.dto.request.RenovarTokenRequest;
import br.com.cortex.sign.modules.auth.dto.response.LoginResponse;
import br.com.cortex.sign.modules.auth.dto.response.UsuarioAutenticadoResponse;
import br.com.cortex.sign.modules.auth.jwt.JwtService;
import br.com.cortex.sign.modules.auth.jwt.JwtTokenGerado;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenAtualizacaoService tokenAtualizacaoService;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(request.email()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));

        validarUsuarioAtivo(usuario);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }

        TokenAtualizacaoGerado tokenAtualizacao = tokenAtualizacaoService.criar(usuario, servletRequest);
        return criarRespostaAutenticacao(usuario, tokenAtualizacao);
    }

    @Transactional
    public LoginResponse renovarToken(RenovarTokenRequest request, HttpServletRequest servletRequest) {
        TokenAtualizacaoRotacionado tokenRotacionado = tokenAtualizacaoService.rotacionar(
                request.tokenAtualizacao(),
                servletRequest
        );

        return criarRespostaAutenticacao(tokenRotacionado.usuario(), tokenRotacionado.tokenAtualizacao());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        tokenAtualizacaoService.revogar(request.tokenAtualizacao());
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoResponse me(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Organizacao organizacao = usuario.getOrganizacao();

        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }

    @Transactional
    public void alterarSenha(UsuarioAutenticado usuarioAutenticado, AlterarSenhaRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Senha atual inválida");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        tokenAtualizacaoService.revogarTodosDoUsuario(usuario);
    }

    private LoginResponse criarRespostaAutenticacao(Usuario usuario, TokenAtualizacaoGerado tokenAtualizacao) {
        JwtTokenGerado tokenAcesso = jwtService.gerarToken(usuario);
        Organizacao organizacao = usuario.getOrganizacao();

        return new LoginResponse(
                tokenAcesso.token(),
                tokenAtualizacao.token(),
                "Bearer",
                tokenAcesso.expiraEm(),
                tokenAtualizacao.expiraEm(),
                usuario.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }

    private Usuario buscarUsuarioAutenticado(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }

        Usuario usuario = usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));

        validarUsuarioAtivo(usuario);

        return usuario;
    }

    private void validarUsuarioAtivo(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
