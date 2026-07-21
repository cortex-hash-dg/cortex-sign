package br.com.cortex.sign.integration.assinatura.cortex;

import br.com.cortex.sign.integration.assinatura.ProvedorAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.DadosAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.ResultadoAssinatura;
import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.assinatura.provider", havingValue = "CORTEX_SIGN", matchIfMissing = true)
public class CortexSignProvedorAssinatura implements ProvedorAssinatura {

    private static final DateTimeFormatter PROTOCOLO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public ResultadoAssinatura assinar(DadosAssinatura dadosAssinatura) {
        LocalDateTime assinadoEm = LocalDateTime.now();
        String termoAceite = montarTermoAceite(dadosAssinatura);
        String termoHash = gerarHashSha256(termoAceite);
        String evidencia = montarEvidencia(dadosAssinatura, assinadoEm, termoHash);
        String evidenciaHash = gerarHashSha256(evidencia);
        String protocolo = "CSIGN-"
                + assinadoEm.format(PROTOCOLO_FORMATTER)
                + "-"
                + evidenciaHash.substring(0, 16).toUpperCase(Locale.ROOT);

        return new ResultadoAssinatura(
                getProviderType(),
                protocolo,
                assinadoEm,
                evidencia,
                evidenciaHash,
                termoAceite
        );
    }

    @Override
    public boolean validar(String protocolo) {
        return protocolo != null && protocolo.startsWith("CSIGN-");
    }

    @Override
    public TipoProvedorAssinatura getProviderType() {
        return TipoProvedorAssinatura.CORTEX_SIGN;
    }

    private String montarTermoAceite(DadosAssinatura dados) {
        return "Declaro que li e concordo em assinar eletronicamente o documento '"
                + dados.documentoTitulo()
                + "' no Cortex Sign. Reconheço que a confirmação por token e código representa minha manifestação de vontade para este documento, vinculada ao hash SHA-256 "
                + dados.documentoHashSha256()
                + ".";
    }

    private String montarEvidencia(DadosAssinatura dados, LocalDateTime assinadoEm, String termoHash) {
        return String.join("\n",
                "versao=1",
                "provedor=CORTEX_SIGN",
                "documentoId=" + dados.documentoId(),
                "documentoTitulo=" + limpar(dados.documentoTitulo()),
                "documentoHashSha256=" + dados.documentoHashSha256(),
                "documentoTamanhoBytes=" + dados.documentoTamanhoBytes(),
                "organizacaoId=" + dados.organizacaoId(),
                "organizacaoNome=" + limpar(dados.organizacaoNome()),
                "solicitacaoAssinaturaId=" + dados.solicitacaoAssinaturaId(),
                "solicitacaoCriadaEm=" + dados.solicitacaoCriadaEm(),
                "solicitacaoExpiraEm=" + dados.solicitacaoExpiraEm(),
                "signatarioId=" + dados.signatarioId(),
                "signatarioNome=" + limpar(dados.signatarioNome()),
                "signatarioEmail=" + limpar(dados.signatarioEmail()),
                "codigoHash=" + dados.codigoHash(),
                "ip=" + limpar(dados.ip()),
                "userAgent=" + limpar(dados.userAgent()),
                "assinadoEm=" + assinadoEm,
                "termoAceiteHashSha256=" + termoHash
        );
    }

    private String limpar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor.replace("\n", " ").replace("\r", " ").trim();
    }

    private String gerarHashSha256(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar hash da evidência de assinatura", exception);
        }
    }
}
