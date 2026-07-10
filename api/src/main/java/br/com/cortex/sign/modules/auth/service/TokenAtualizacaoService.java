package br.com.cortex.sign.modules.auth.service;

import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.modules.auth.entity.TokenAtualizacao;
import br.com.cortex.sign.modules.auth.repository.TokenAtualizacaoRepository;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenAtualizacaoService {

    private static final int TOKEN_BYTES = 48;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TokenAtualizacaoRepository tokenAtualizacaoRepository;

    @Value("${app.jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @Transactional
    public TokenAtualizacaoGerado criar(Usuario usuario, HttpServletRequest request) {
        String token = gerarTokenSeguro();
        LocalDateTime expiraEm = LocalDateTime.now().plusDays(refreshExpirationDays);

        TokenAtualizacao tokenAtualizacao = new TokenAtualizacao();
        tokenAtualizacao.setUsuario(usuario);
        tokenAtualizacao.setTokenHash(gerarHash(token));
        tokenAtualizacao.setExpiraEm(expiraEm);
        tokenAtualizacao.setIpCriacao(obterIp(request));
        tokenAtualizacao.setUserAgent(limitar(obterUserAgent(request), 500));

        tokenAtualizacaoRepository.save(tokenAtualizacao);

        return new TokenAtualizacaoGerado(token, expiraEm);
    }

    @Transactional
    public TokenAtualizacaoRotacionado rotacionar(String token, HttpServletRequest request) {
        TokenAtualizacao tokenAtualizacao = buscarTokenValido(token);
        LocalDateTime agora = LocalDateTime.now();

        tokenAtualizacao.setUltimoUsoEm(agora);
        tokenAtualizacao.setRevogadoEm(agora);

        Usuario usuario = tokenAtualizacao.getUsuario();
        TokenAtualizacaoGerado novoToken = criar(usuario, request);

        return new TokenAtualizacaoRotacionado(usuario, novoToken);
    }

    @Transactional
    public void revogar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        tokenAtualizacaoRepository.findByTokenHash(gerarHash(token))
                .filter(tokenAtualizacao -> tokenAtualizacao.getRevogadoEm() == null)
                .ifPresent(tokenAtualizacao -> tokenAtualizacao.setRevogadoEm(LocalDateTime.now()));
    }

    @Transactional
    public void revogarTodosDoUsuario(Usuario usuario) {
        tokenAtualizacaoRepository.findAllByUsuarioIdAndRevogadoEmIsNull(usuario.getId())
                .forEach(tokenAtualizacao -> tokenAtualizacao.setRevogadoEm(LocalDateTime.now()));
    }

    private TokenAtualizacao buscarTokenValido(String token) {
        TokenAtualizacao tokenAtualizacao = tokenAtualizacaoRepository.findByTokenHash(gerarHash(token))
                .orElseThrow(() -> new CredenciaisInvalidasException("Refresh token inválido"));

        if (!tokenAtualizacao.estaAtivo()) {
            throw new CredenciaisInvalidasException("Refresh token inválido");
        }

        if (!Boolean.TRUE.equals(tokenAtualizacao.getUsuario().getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }

        return tokenAtualizacao;
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String gerarHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o token de atualização", exception);
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
