package br.com.cortex.sign.modules.certificado.service;

import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.certificado.dto.response.CertificadoDocumentoValidacaoResponse;
import br.com.cortex.sign.modules.certificado.dto.response.CertificadoValidacaoPublicaResponse;
import br.com.cortex.sign.modules.certificado.entity.Certificado;
import br.com.cortex.sign.modules.certificado.repository.CertificadoRepository;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CertificadoService {

    private static final Pattern XSIGN_CERTIFICADO_PATTERN = Pattern.compile("Xsign\\s+([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final Pattern HASH_PATTERN = Pattern.compile("[a-fA-F0-9]{64}");

    private final CertificadoRepository certificadoRepository;

    @Transactional(readOnly = true)
    public CertificadoValidacaoPublicaResponse verificar(UUID id) {
        Certificado certificado = certificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Certificado não encontrado"));

        return montarResponse(certificado);
    }

    @Transactional(readOnly = true)
    public CertificadoDocumentoValidacaoResponse verificarDocumento(MultipartFile arquivo, UUID certificadoIdInformado) {
        try {
            byte[] bytes = arquivo.getBytes();
            String hashArquivoEnviado = gerarHashSha256(bytes);
            String textoPdf = extrairTextoPdf(bytes);
            UUID certificadoId = certificadoIdInformado != null ? certificadoIdInformado : extrairCertificadoId(textoPdf);

            if (certificadoId == null) {
                return new CertificadoDocumentoValidacaoResponse(
                        null,
                        false,
                        false,
                        false,
                        false,
                        hashArquivoEnviado,
                        null,
                        extrairPrimeiroHash(textoPdf),
                        null,
                        null,
                        "Não foi possível encontrar um ID de certificado Xsign no PDF enviado.",
                        null
                );
            }

            Certificado certificado = certificadoRepository.findById(certificadoId)
                    .orElse(null);

            if (certificado == null) {
                return new CertificadoDocumentoValidacaoResponse(
                        certificadoId,
                        false,
                        true,
                        false,
                        false,
                        hashArquivoEnviado,
                        null,
                        extrairPrimeiroHash(textoPdf),
                        null,
                        null,
                        "O PDF contém um ID de certificado, mas ele não foi encontrado no banco.",
                        null
                );
            }

            String hashDocumentoExtraido = extrairHashDocumento(textoPdf, certificado.getHashDocumento());
            String hashArquivoAssinadoRegistrado = extrairDadoAutenticacao(certificado.getDadosAutenticacao(), "arquivoAssinadoHashSha256");
            boolean documentoPossuiCertificado = textoPdf.contains(certificado.getId().toString());
            boolean hashDocumentoConfere = certificado.getHashDocumento() != null && textoPdf.contains(certificado.getHashDocumento());
            boolean hashArquivoAssinadoConfere = hashArquivoAssinadoRegistrado != null && hashArquivoAssinadoRegistrado.equalsIgnoreCase(hashArquivoEnviado);
            CertificadoValidacaoPublicaResponse certificadoResponse = montarResponse(certificado);

            String mensagem = documentoPossuiCertificado && (hashDocumentoConfere || hashArquivoAssinadoConfere)
                    ? "Documento validado com evidências compatíveis com o certificado Xsign."
                    : "Certificado encontrado, mas o PDF enviado não possui todas as evidências esperadas.";

            return new CertificadoDocumentoValidacaoResponse(
                    certificado.getId(),
                    true,
                    documentoPossuiCertificado,
                    hashDocumentoConfere,
                    hashArquivoAssinadoConfere,
                    hashArquivoEnviado,
                    certificado.getHashDocumento(),
                    hashDocumentoExtraido,
                    hashArquivoAssinadoRegistrado,
                    certificado.getAssinaturaDigital(),
                    mensagem,
                    certificadoResponse
            );
        } catch (Exception exception) {
            throw new RecursoNaoEncontradoException("Não foi possível validar o documento enviado");
        }
    }
    private CertificadoValidacaoPublicaResponse montarResponse(Certificado certificado) {
        Assinatura assinatura = certificado.getAssinatura();
        return new CertificadoValidacaoPublicaResponse(
                certificado.getId(),
                certificado.getDocumento().getId(),
                assinatura.getId(),
                assinatura.getStatus(),
                assinatura.getProvedor(),
                assinatura.getProtocolo(),
                assinatura.getAssinadoEm(),
                certificado.getHashDocumento(),
                certificado.getAssinaturaDigital(),
                certificado.getCriadoEm()
        );
    }

    private String extrairTextoPdf(byte[] bytes) throws Exception {
        try (var document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private UUID extrairCertificadoId(String textoPdf) {
        Matcher matcher = XSIGN_CERTIFICADO_PATTERN.matcher(textoPdf);
        if (matcher.find()) {
            return UUID.fromString(matcher.group(1));
        }

        matcher = UUID_PATTERN.matcher(textoPdf);
        while (matcher.find()) {
            UUID uuid = UUID.fromString(matcher.group());
            if (certificadoRepository.existsById(uuid)) {
                return uuid;
            }
        }

        return null;
    }

    private String extrairHashDocumento(String textoPdf, String hashRegistrado) {
        if (hashRegistrado != null && textoPdf.contains(hashRegistrado)) {
            return hashRegistrado;
        }

        return extrairPrimeiroHash(textoPdf);
    }

    private String extrairPrimeiroHash(String textoPdf) {
        Matcher matcher = HASH_PATTERN.matcher(textoPdf);
        return matcher.find() ? matcher.group() : null;
    }

    private String extrairDadoAutenticacao(String dadosAutenticacao, String chave) {
        if (dadosAutenticacao == null || dadosAutenticacao.isBlank()) {
            return null;
        }

        String prefixo = chave + "=";
        for (String linha : dadosAutenticacao.split("\\n")) {
            if (linha.startsWith(prefixo)) {
                return linha.substring(prefixo.length()).trim();
            }
        }

        return null;
    }

    private String gerarHashSha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

}
