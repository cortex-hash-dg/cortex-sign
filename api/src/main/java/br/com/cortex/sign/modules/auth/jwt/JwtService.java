package br.com.cortex.sign.modules.auth.jwt;

import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper objectMapper;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-minutes:120}")
    private long expirationMinutes;

    @PostConstruct
    void validarConfiguracao() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 32 caracteres");
        }
    }

    public JwtTokenGerado gerarToken(Usuario usuario) {
        Instant emitidoEm = Instant.now();
        Instant expiraEm = emitidoEm.plusSeconds(expirationMinutes * 60);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", usuario.getId().toString());
        payload.put("nome", usuario.getNome());
        payload.put("email", usuario.getEmail());
        payload.put("perfil", usuario.getPerfil().name());
        payload.put("iat", emitidoEm.getEpochSecond());
        payload.put("exp", expiraEm.getEpochSecond());

        Organizacao organizacao = usuario.getOrganizacao();
        if (organizacao != null) {
            payload.put("organizacaoId", organizacao.getId().toString());
        }

        String headerBase64 = toBase64Url(header);
        String payloadBase64 = toBase64Url(payload);
        String conteudoAssinado = headerBase64 + "." + payloadBase64;
        String assinatura = assinar(conteudoAssinado);

        return new JwtTokenGerado(
                conteudoAssinado + "." + assinatura,
                LocalDateTime.ofInstant(expiraEm, ZoneId.systemDefault())
        );
    }

    public Optional<UsuarioAutenticado> validarToken(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return Optional.empty();
            }

            String conteudoAssinado = partes[0] + "." + partes[1];
            String assinaturaEsperada = assinar(conteudoAssinado);
            if (!assinaturasIguais(assinaturaEsperada, partes[2])) {
                return Optional.empty();
            }

            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(partes[1]),
                    new TypeReference<>() {
                    }
            );

            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= exp) {
                return Optional.empty();
            }

            UUID organizacaoId = payload.containsKey("organizacaoId")
                    ? UUID.fromString((String) payload.get("organizacaoId"))
                    : null;

            return Optional.of(new UsuarioAutenticado(
                    UUID.fromString((String) payload.get("sub")),
                    organizacaoId,
                    (String) payload.get("nome"),
                    (String) payload.get("email"),
                    PerfilUsuario.valueOf((String) payload.get("perfil"))
            ));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String toBase64Url(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o token JWT", exception);
        }
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec chave = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(chave);
            byte[] assinatura = mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar o token JWT", exception);
        }
    }

    private boolean assinaturasIguais(String assinaturaEsperada, String assinaturaRecebida) {
        return MessageDigest.isEqual(
                assinaturaEsperada.getBytes(StandardCharsets.UTF_8),
                assinaturaRecebida.getBytes(StandardCharsets.UTF_8)
        );
    }
}
