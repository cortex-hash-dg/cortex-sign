package br.com.cortex.sign.modules.assinatura.service;

import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.ArmazenamentoException;
import br.com.cortex.sign.integration.storage.StorageProvider;
import br.com.cortex.sign.integration.storage.dto.ArquivoUploadResultado;
import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.assinatura.repository.AssinaturaRepository;
import br.com.cortex.sign.modules.assinatura.repository.SolicitacaoAssinaturaRepository;
import br.com.cortex.sign.modules.certificado.entity.Certificado;
import br.com.cortex.sign.modules.certificado.repository.CertificadoRepository;
import br.com.cortex.sign.modules.documento.entity.ArquivoArmazenado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.entity.DocumentoVersao;
import br.com.cortex.sign.modules.documento.repository.ArquivoArmazenadoRepository;
import br.com.cortex.sign.modules.documento.repository.DocumentoRepository;
import br.com.cortex.sign.modules.documento.repository.DocumentoVersaoRepository;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoAssinadoService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final Color AZUL_MARINHO = new Color(6, 38, 85);
    private static final Color AZUL_PRINCIPAL = new Color(8, 126, 209);
    private static final Color AZUL_CLARO = new Color(234, 245, 253);
    private static final Color FUNDO = new Color(248, 251, 255);
    private static final Color SUPERFICIE = new Color(255, 255, 255);
    private static final Color BORDA = new Color(220, 228, 238);
    private static final Color TEXTO = new Color(8, 35, 69);
    private static final Color TEXTO_SECUNDARIO = new Color(96, 112, 137);
    private static final Color SUCESSO = new Color(25, 148, 71);

    private final StorageProvider storageProvider;
    private final ArquivoArmazenadoRepository arquivoArmazenadoRepository;
    private final DocumentoRepository documentoRepository;
    private final DocumentoVersaoRepository documentoVersaoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final SolicitacaoAssinaturaRepository solicitacaoRepository;
    private final CertificadoRepository certificadoRepository;

    @Value("${app.certificado.verificacao.base-url:http://localhost:5173/verificar/certificado}")
    private String baseUrlVerificacao;

    public void registrarAssinaturaVisual(Documento documento, Assinatura assinatura) {
        try {
            UUID certificadoId = UUID.randomUUID();
            String urlVerificacao = montarUrlVerificacao(certificadoId);
            ArquivoArmazenado arquivoOriginal = buscarArquivoOriginal(documento);
            Resource resource = storageProvider.download(arquivoOriginal.getCaminho());
            List<SolicitacaoAssinatura> solicitacoes = solicitacaoRepository.findAllByDocumentoIdOrderByCriadoEmAsc(documento.getId());
            List<Assinatura> assinaturas = assinaturaRepository.findAllBySolicitacaoAssinaturaDocumentoIdOrderByCriadoEmAsc(documento.getId());
            byte[] pdfAssinado;

            try (InputStream inputStream = resource.getInputStream()) {
                pdfAssinado = carimbarPdf(
                        inputStream.readAllBytes(),
                        assinatura,
                        certificadoId,
                        urlVerificacao,
                        solicitacoes,
                        assinaturas
                );
            }

            String nomeOriginal = resolverNomeAssinado(arquivoOriginal.getNomeOriginal());
            ArquivoUploadResultado upload = storageProvider.upload(
                    pdfAssinado,
                    nomeOriginal,
                    PDF_CONTENT_TYPE,
                    "organizacoes/" + documento.getOrganizacao().getId() + "/documentos/assinados"
            );

            ArquivoArmazenado arquivoAssinado = salvarArquivo(upload);
            documento.setArquivoAtual(arquivoAssinado);
            documentoRepository.saveAndFlush(documento);
            registrarNovaVersao(documento, arquivoAssinado);
            registrarCertificado(certificadoId, documento, assinatura, upload, urlVerificacao);
        } catch (ArmazenamentoException exception) {
            log.warn("Falha de armazenamento ao gravar manifesto do documento {}: {}", documento.getId(), exception.getMessage(), exception);
            throw exception;
        } catch (Exception exception) {
            log.warn("Falha ao gravar manifesto do documento {}: {}", documento.getId(), exception.getMessage(), exception);
            throw new ConflitoException("Não foi possível gravar o manifesto de assinatura no PDF");
        }
    }

    private byte[] carimbarPdf(
            byte[] pdfOriginal,
            Assinatura assinatura,
            UUID certificadoId,
            String urlVerificacao,
            List<SolicitacaoAssinatura> solicitacoes,
            List<Assinatura> assinaturas
    ) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(pdfOriginal);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int totalPaginasOriginais = pdf.getNumberOfPages();
            PDRectangle formatoCertificado = copiarFormatoPagina(pdf.getPage(0).getMediaBox());
            PDImageXObject logo = carregarLogo(pdf);
            PDImageXObject qrCode = gerarQrCode(pdf, urlVerificacao);

            for (int index = 0; index < totalPaginasOriginais; index++) {
                PDPage page = pdf.getPage(index);
                carimbarHashLateral(pdf, page, assinatura, certificadoId, urlVerificacao, index + 1, totalPaginasOriginais);
            }

            adicionarPaginaManifesto(pdf, assinatura, logo, qrCode, certificadoId, urlVerificacao, formatoCertificado, totalPaginasOriginais, solicitacoes, assinaturas);
            pdf.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private PDRectangle copiarFormatoPagina(PDRectangle mediaBox) {
        return new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight());
    }

    private PDImageXObject carregarLogo(PDDocument pdf) throws Exception {
        ClassPathResource logoResource = new ClassPathResource("brand/xsign-logo.png");
        if (!logoResource.exists()) {
            return null;
        }

        try (InputStream inputStream = logoResource.getInputStream()) {
            return PDImageXObject.createFromByteArray(pdf, inputStream.readAllBytes(), "xsign-logo");
        }
    }

    private PDImageXObject gerarQrCode(PDDocument pdf, String conteudo) throws Exception {
        int tamanho = 220;
        Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name(),
                EncodeHintType.MARGIN, 1
        );
        BitMatrix matrix = new MultiFormatWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho, hints);
        BufferedImage image = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < tamanho; y++) {
            for (int x = 0; x < tamanho; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }

        return LosslessFactory.createFromImage(pdf, image);
    }

    private void carimbarHashLateral(
            PDDocument pdf,
            PDPage page,
            Assinatura assinatura,
            UUID certificadoId,
            String urlVerificacao,
            int paginaAtual,
            int totalPaginas
    ) throws Exception {
        PDRectangle mediaBox = page.getMediaBox();
        float width = mediaBox.getWidth();
        float height = mediaBox.getHeight();
        float x = width - 14;
        float y = 42;
        String textoLateral = "Xsign " + certificadoId
                + " | Documento assinado eletronicamente"
                + " | Hash " + hashCurto(assinatura.getDocumentoHashSha256())
                + " | Pag. " + paginaAtual + "/" + totalPaginas;

        try (PDPageContentStream content = new PDPageContentStream(
                pdf,
                page,
                PDPageContentStream.AppendMode.APPEND,
                true,
                true
        )) {
            content.setStrokingColor(new Color(224, 224, 224));
            content.setLineWidth(0.35f);
            content.moveTo(width - 19, y - 8);
            content.lineTo(width - 19, height - 42);
            content.stroke();

            desenharTextoRotacionado(
                    content,
                    limitarPorLargura(textoLateral, FONT_REGULAR, 6.1f, height - 84),
                    FONT_REGULAR,
                    6.1f,
                    new Color(120, 120, 120),
                    x,
                    y,
                    Math.PI / 2
            );
        }
    }

    private void adicionarPaginaManifesto(
            PDDocument pdf,
            Assinatura assinatura,
            PDImageXObject logo,
            PDImageXObject qrCode,
            UUID certificadoId,
            String urlVerificacao,
            PDRectangle formatoPagina,
            int paginasDocumento,
            List<SolicitacaoAssinatura> solicitacoes,
            List<Assinatura> assinaturas
    ) throws Exception {
        PDPage pagina = new PDPage(formatoPagina);
        pdf.addPage(pagina);

        try (PDPageContentStream content = new PDPageContentStream(pdf, pagina)) {
            float width = formatoPagina.getWidth();
            float height = formatoPagina.getHeight();
            float margin = Math.max(28, width * 0.06f);
            float contentWidth = width - (margin * 2);
            List<Assinatura> assinaturasManifesto = assinaturasManifesto(assinatura, assinaturas);

            desenharFundoRelatorioAssinaturas(content, width, height);
            desenharCabecalhoRelatorioAssinaturas(content, logo, assinatura, width, height, margin);
            desenharResumoDocumento(content, assinatura, certificadoId, urlVerificacao, margin, height - 122, contentWidth, paginasDocumento);

            float cardY = height - 255;
            float cardHeight = 124;
            int limite = Math.min(assinaturasManifesto.size(), 3);
            for (int index = 0; index < limite; index++) {
                desenharBlocoRelatorioAssinatura(
                        pdf,
                        content,
                        assinaturasManifesto.get(index),
                        margin,
                        cardY,
                        contentWidth,
                        cardHeight
                );
                cardY -= cardHeight + 8;
            }

            float eventosY = Math.max(122, cardY + 36);
            desenharEventosCompactos(content, assinatura, solicitacoes, assinaturasManifesto, margin, eventosY, contentWidth);

            if (assinaturasManifesto.size() > limite) {
                desenharTexto(content, "+ " + (assinaturasManifesto.size() - limite) + " assinatura(s) adicional(is) registradas nos logs.", FONT_REGULAR, 6.4f, new Color(84, 84, 84), margin, eventosY - 58);
            }

            desenharRodapeRelatorioAssinaturas(content, qrCode, certificadoId, urlVerificacao, margin, 28, contentWidth);
        }
    }

    private void desenharFundoRelatorioAssinaturas(PDPageContentStream content, float width, float height) throws Exception {
        content.setNonStrokingColor(SUPERFICIE);
        content.addRect(0, 0, width, height);
        content.fill();
    }

    private void desenharCabecalhoRelatorioAssinaturas(PDPageContentStream content, PDImageXObject logo, Assinatura assinatura, float width, float height, float margin) throws Exception {
        desenharTexto(content, "Relatório de Assinaturas", FONT_BOLD, 17f, new Color(55, 55, 55), margin, height - 38);
        desenharTexto(content, "Datas e horários em UTC-0300", FONT_REGULAR, 9.2f, new Color(72, 72, 72), margin, height - 57);
        desenharTexto(content, "Última atualização em " + formatarData(assinatura.getCriadoEm()), FONT_REGULAR, 9.2f, new Color(72, 72, 72), margin, height - 72);

        if (logo != null) {
            float logoWidth = 98;
            float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
            content.drawImage(logo, width - margin - logoWidth, height - 58, logoWidth, logoHeight);
        } else {
            desenharTexto(content, "Xsign", FONT_BOLD, 24f, AZUL_MARINHO, width - margin - 82, height - 44);
        }

        content.setStrokingColor(new Color(210, 210, 210));
        content.setLineWidth(0.45f);
        content.moveTo(0, height - 86);
        content.lineTo(width, height - 86);
        content.stroke();
    }

    private void desenharResumoDocumento(PDPageContentStream content, Assinatura assinatura, UUID certificadoId, String urlVerificacao, float x, float y, float width, int paginasDocumento) throws Exception {
        Documento documento = assinatura.getSolicitacaoAssinatura().getDocumento();
        desenharTexto(content, "Documento", FONT_BOLD, 8.2f, new Color(64, 64, 64), x, y);
        desenharTexto(content, limitarPorLargura(valor(documento.getTitulo()), FONT_REGULAR, 8.2f, width - 90), FONT_REGULAR, 8.2f, new Color(64, 64, 64), x + 70, y);
        desenharTexto(content, "Código", FONT_BOLD, 7.2f, new Color(96, 96, 96), x, y - 15);
        desenharTexto(content, valor(documento.getId()), FONT_REGULAR, 7.2f, new Color(96, 96, 96), x + 70, y - 15);
        desenharTexto(content, "Verificação", FONT_BOLD, 7.2f, new Color(96, 96, 96), x, y - 30);
        desenharTexto(content, limitarPorLargura(urlVerificacao, FONT_REGULAR, 7.2f, width - 90), FONT_REGULAR, 7.2f, new Color(96, 96, 96), x + 70, y - 30);
        desenharTexto(content, "Hash SHA-256", FONT_BOLD, 7.2f, new Color(96, 96, 96), x, y - 45);
        desenharTexto(content, limitarPorLargura(valor(assinatura.getDocumentoHashSha256()), FONT_REGULAR, 7.2f, width - 90), FONT_REGULAR, 7.2f, new Color(96, 96, 96), x + 70, y - 45);
        desenharTexto(content, "Páginas originais: " + paginasDocumento, FONT_REGULAR, 7.2f, new Color(96, 96, 96), x, y - 60);
    }

    private List<Assinatura> assinaturasManifesto(Assinatura assinaturaAtual, List<Assinatura> assinaturas) {
        return assinaturas == null || assinaturas.isEmpty() ? List.of(assinaturaAtual) : assinaturas;
    }

    private void desenharBlocoRelatorioAssinatura(PDDocument pdf, PDPageContentStream content, Assinatura assinatura, float x, float y, float width, float height) throws Exception {
        Signatario signatario = assinatura.getSolicitacaoAssinatura().getSignatario();
        float assinaturaColWidth = 166;
        float assinaturaX = x + width - assinaturaColWidth;
        float topoHeight = 70;

        content.setNonStrokingColor(SUPERFICIE);
        content.addRect(x, y, width, height);
        content.fill();
        content.setStrokingColor(new Color(215, 215, 215));
        content.setLineWidth(0.6f);
        content.addRect(x, y, width, height);
        content.stroke();

        content.setStrokingColor(new Color(218, 218, 218));
        content.setLineWidth(0.45f);
        content.moveTo(assinaturaX - 8, y + height);
        content.lineTo(assinaturaX - 8, y + height - topoHeight);
        content.stroke();
        content.moveTo(x, y + height - topoHeight);
        content.lineTo(x + width, y + height - topoHeight);
        content.stroke();

        float nomeY = y + height - 34;
        float pillWidth = 30;
        float pillHeight = 10;
        float pillGap = 8;
        float nomeX = x + 10;
        float pillX = assinaturaX - 18 - pillWidth;
        float nomeMaxWidth = Math.max(50, pillX - nomeX - pillGap);

        desenharTexto(content, limitarPorLargura(valor(signatario.getNome()).toUpperCase(), FONT_BOLD, 9.6f, nomeMaxWidth), FONT_BOLD, 9.6f, new Color(48, 48, 48), nomeX, nomeY);
        desenharPill(content, pillX, nomeY - 3, pillWidth, pillHeight, "Assinado", SUCESSO);
        desenharTexto(content, "Data e hora da assinatura: " + formatarData(assinatura.getAssinadoEm()), FONT_REGULAR, 6.8f, new Color(64, 64, 64), x + 10, y + height - 50);
        desenharTexto(content, "Token: " + valorOuPadrao(assinatura.getProtocolo(), hashCurto(assinatura.getSolicitacaoAssinatura().getCodigoHash())), FONT_REGULAR, 6.8f, new Color(64, 64, 64), x + 10, y + height - 62);

        desenharTexto(content, "Assinatura", FONT_REGULAR, 7f, new Color(64, 64, 64), assinaturaX + 8, y + height - 15);
        PDImageXObject imagemAssinatura = carregarAssinaturaManuscrita(pdf, assinatura);
        if (imagemAssinatura != null) {
            desenharImagemProporcional(content, imagemAssinatura, assinaturaX + 34, y + height - 51, assinaturaColWidth - 76, 25);
        } else {
            desenharTexto(content, "Assinado digitalmente", FONT_REGULAR, 7f, new Color(90, 90, 90), assinaturaX + 26, y + height - 39);
        }
        desenharTexto(content, limitarPorLargura(valor(signatario.getNome()), FONT_REGULAR, 7f, assinaturaColWidth - 20), FONT_REGULAR, 7f, new Color(64, 64, 64), assinaturaX + 8, y + height - 56);

        float authY = y + height - topoHeight - 14;
        desenharTexto(content, "Pontos de autenticação:", FONT_BOLD, 7.4f, new Color(55, 55, 55), x + 10, authY);
        desenharTexto(content, "E-mail: " + limitarPorLargura(valor(signatario.getEmail()), FONT_REGULAR, 6.7f, (width / 2) - 45), FONT_REGULAR, 6.7f, new Color(64, 64, 64), x + 10, authY - 14);
        desenharTexto(content, "Documento: " + valorOuPadrao(signatario.getNumeroDocumento(), "Não informado"), FONT_REGULAR, 6.7f, new Color(64, 64, 64), x + 10, authY - 27);

        float rightX = x + (width / 2);
        desenharTexto(content, "IP: " + valorOuPadrao(assinatura.getIpAssinatura(), "Não informado"), FONT_REGULAR, 6.7f, new Color(64, 64, 64), rightX, authY);
        desenharTexto(content, "Hash do documento: " + hashCurto(assinatura.getDocumentoHashSha256()), FONT_REGULAR, 6.7f, new Color(64, 64, 64), rightX, authY - 14);
        desenharTexto(content, "Hash da evidência: " + hashCurto(assinatura.getEvidenciaHashSha256()), FONT_REGULAR, 6.7f, new Color(64, 64, 64), rightX, authY - 27);
    }

    private void desenharPill(PDPageContentStream content, float x, float y, float width, float height, String texto, Color cor) throws Exception {
        Color fundoPill = new Color(219, 248, 228);
        float radius = height / 2f;
        float right = x + width;
        float top = y + height;
        float middleY = y + radius;
        float control = radius * 0.55228475f;

        content.setNonStrokingColor(fundoPill);
        content.moveTo(x + radius, y);
        content.lineTo(right - radius, y);
        content.curveTo(right - radius + control, y, right, middleY - control, right, middleY);
        content.curveTo(right, middleY + control, right - radius + control, top, right - radius, top);
        content.lineTo(x + radius, top);
        content.curveTo(x + radius - control, top, x, middleY + control, x, middleY);
        content.curveTo(x, middleY - control, x + radius - control, y, x + radius, y);
        content.fill();

        desenharTexto(content, texto, FONT_REGULAR, 4.8f, new Color(31, 110, 61), x + 5, y + 3);
    }

    private void desenharEventosCompactos(PDPageContentStream content, Assinatura assinaturaAtual, List<SolicitacaoAssinatura> solicitacoes, List<Assinatura> assinaturas, float x, float y, float width) throws Exception {
        desenharTexto(content, "Eventos do documento", FONT_BOLD, 7.5f, new Color(55, 55, 55), x, y);
        List<EventoManifesto> eventos = montarEventos(assinaturaAtual, solicitacoes, assinaturas);
        float linhaY = y - 13;
        int limite = Math.min(eventos.size(), 4);

        for (int index = 0; index < limite; index++) {
            EventoManifesto evento = eventos.get(index);
            desenharTexto(content, limitarPorLargura(formatarData(evento.data()) + " - " + evento.descricao(), FONT_REGULAR, 6.2f, width), FONT_REGULAR, 6.2f, new Color(82, 82, 82), x, linhaY);
            linhaY -= 11;
        }
    }

    private void desenharRodapeRelatorioAssinaturas(PDPageContentStream content, PDImageXObject qrCode, UUID certificadoId, String urlVerificacao, float x, float y, float width) throws Exception {
        content.setStrokingColor(new Color(190, 190, 190));
        content.setLineWidth(0.5f);
        content.moveTo(0, y + 68);
        content.lineTo(x + width + x, y + 68);
        content.stroke();

        desenharTexto(content, "INTEGRIDADE CERTIFICADA - XSIGN", FONT_BOLD, 9.5f, new Color(45, 45, 45), x, y + 48);
        desenharTexto(content, "Assinaturas eletrônicas possuem validade jurídica conforme MP 2.200-2/2001 e Lei 14.063/2020.", FONT_REGULAR, 6.8f, new Color(64, 64, 64), x, y + 34);
        desenharTexto(content, "Confira a integridade do documento pelo QR Code ou pelo link de verificação.", FONT_BOLD, 6.8f, new Color(64, 64, 64), x, y + 22);
        desenharTexto(content, limitarPorLargura(urlVerificacao, FONT_REGULAR, 6.1f, width - 68), FONT_REGULAR, 6.1f, new Color(64, 64, 64), x, y + 10);

        if (qrCode != null) {
            content.drawImage(qrCode, x + width - 48, y + 17, 42, 42);
        }

        desenharTexto(content, "Xsign " + certificadoId + ". Documento assinado eletronicamente.", FONT_REGULAR, 6.1f, new Color(130, 130, 130), x, y - 6);
    }

    private void desenharFundoManifesto(PDPageContentStream content, float width, float height) throws Exception {
        content.setNonStrokingColor(FUNDO);
        content.addRect(0, 0, width, height);
        content.fill();

        content.setNonStrokingColor(AZUL_CLARO);
        content.addRect(0, height - 112, width, 112);
        content.fill();

        content.setNonStrokingColor(AZUL_MARINHO);
        content.addRect(0, height - 7, width, 7);
        content.fill();

        content.setNonStrokingColor(AZUL_PRINCIPAL);
        content.addRect(0, height - 7, width * 0.42f, 7);
        content.fill();
    }

    private void desenharCabecalho(PDPageContentStream content, PDImageXObject logo, float width, float height, float margin) throws Exception {
        if (logo != null) {
            float logoWidth = 104;
            float logoHeight = logoWidth * logo.getHeight() / logo.getWidth();
            content.drawImage(logo, margin, height - 56, logoWidth, logoHeight);
        } else {
            desenharTexto(content, "Xsign", FONT_BOLD, 20f, AZUL_MARINHO, margin, height - 47);
        }

        desenharTexto(content, "Documento assinado", FONT_BOLD, 10f, AZUL_MARINHO, width - margin - 112, height - 42);
        desenharTexto(content, "Evidência eletrônica", FONT_REGULAR, 7.4f, TEXTO_SECUNDARIO, width - margin - 112, height - 54);
    }

    private void desenharSelo(PDPageContentStream content, float x, float y, float width, float height, String texto) throws Exception {
        content.setNonStrokingColor(new Color(232, 247, 238));
        content.addRect(x, y, width, height);
        content.fill();

        content.setStrokingColor(SUCESSO);
        content.setLineWidth(0.5f);
        content.addRect(x, y, width, height);
        content.stroke();

        desenharTexto(content, texto, FONT_BOLD, 6.8f, SUCESSO, x + 42, y + 9);
    }

    private void desenharCardDadosDocumento(
            PDPageContentStream content,
            Assinatura assinatura,
            UUID certificadoId,
            float x,
            float y,
            float width,
            float height,
            int paginasDocumento
    ) throws Exception {
        Documento documento = assinatura.getSolicitacaoAssinatura().getDocumento();
        desenharCard(content, x, y, width, height, "Dados do documento");
        float valorWidth = width - 32;
        desenharLinhaInfo(content, "Título", valor(documento.getTitulo()), x + 16, y + height - 39, valorWidth);
        desenharLinhaInfo(content, "Código do documento", valor(documento.getId()), x + 16, y + height - 63, valorWidth);
        desenharLinhaInfo(content, "ID de verificação", valor(certificadoId), x + 16, y + height - 87, valorWidth);
        desenharLinhaInfo(content, "Hash SHA-256 do conteúdo", valor(assinatura.getDocumentoHashSha256()), x + 16, y + height - 111, valorWidth);
        desenharLinhaInfo(content, "Páginas originais", String.valueOf(paginasDocumento), x + 16, y + height - 135, valorWidth);
    }

    private void desenharCardQrCode(PDPageContentStream content, PDImageXObject qrCode, String urlVerificacao, float x, float y, float width, float height) throws Exception {
        desenharCard(content, x, y, width, height, "Validação");
        if (qrCode != null) {
            content.drawImage(qrCode, x + 25, y + 36, 74, 74);
        }
        desenharTexto(content, "Escaneie para validar", FONT_BOLD, 6.5f, AZUL_MARINHO, x + 21, y + 23);
        desenharTexto(content, limitarPorLargura(urlVerificacao, FONT_REGULAR, 5.2f, width - 28), FONT_REGULAR, 5.2f, TEXTO_SECUNDARIO, x + 14, y + 12);
    }

    private void desenharCardAssinaturas(PDDocument pdf, PDPageContentStream content, Assinatura assinaturaAtual, List<Assinatura> assinaturas, float x, float y, float width, float height) throws Exception {
        List<Assinatura> assinaturasManifesto = assinaturas == null || assinaturas.isEmpty() ? List.of(assinaturaAtual) : assinaturas;
        desenharCard(content, x, y, width, height, "Manifesto visual das assinaturas");

        float rowY = y + height - 48;
        int limite = Math.min(assinaturasManifesto.size(), 3);
        for (int index = 0; index < limite; index++) {
            Assinatura assinatura = assinaturasManifesto.get(index);
            PDImageXObject assinaturaManuscrita = carregarAssinaturaManuscrita(pdf, assinatura);
            desenharAssinatura(content, assinatura, assinaturaManuscrita, x + 16, rowY, width - 32);
            rowY -= 38;
        }

        if (assinaturasManifesto.size() > limite) {
            desenharTexto(content, "+ " + (assinaturasManifesto.size() - limite) + " assinatura(s) adicional(is) registrada(s).", FONT_REGULAR, 6.4f, TEXTO_SECUNDARIO, x + 16, y + 12);
        }
    }

    private void desenharAssinatura(PDPageContentStream content, Assinatura assinatura, PDImageXObject assinaturaManuscrita, float x, float y, float width) throws Exception {
        Signatario signatario = assinatura.getSolicitacaoAssinatura().getSignatario();
        float assinaturaWidth = 118;
        float textX = x + assinaturaWidth + 18;

        if (assinaturaManuscrita != null) {
            desenharImagemProporcional(content, assinaturaManuscrita, x + 4, y - 15, assinaturaWidth - 10, 28);
        } else {
            desenharTexto(content, "Assinatura eletrônica", FONT_REGULAR, 7.2f, TEXTO_SECUNDARIO, x + 12, y);
        }

        content.setStrokingColor(new Color(155, 166, 184));
        content.setLineWidth(0.45f);
        content.moveTo(x + 2, y - 17);
        content.lineTo(x + assinaturaWidth - 6, y - 17);
        content.stroke();

        desenharTexto(content, limitarPorLargura(valor(signatario.getNome()), FONT_BOLD, 7.5f, assinaturaWidth - 8), FONT_BOLD, 7.5f, TEXTO, x + 3, y - 28);
        if (signatario.getNumeroDocumento() != null && !signatario.getNumeroDocumento().isBlank()) {
            desenharTexto(content, limitarPorLargura(signatario.getNumeroDocumento(), FONT_REGULAR, 6.6f, assinaturaWidth - 8), FONT_REGULAR, 6.6f, TEXTO, x + 3, y - 38);
        }

        desenharTexto(content, "Assinado digitalmente na Xsign por", FONT_REGULAR, 7f, TEXTO_SECUNDARIO, textX, y + 4);
        desenharTexto(content, limitarPorLargura(valor(signatario.getNome()), FONT_BOLD, 8.6f, width - assinaturaWidth - 30), FONT_BOLD, 8.6f, TEXTO, textX, y - 8);
        desenharTexto(content, "Data: " + formatarDataComFuso(assinatura.getAssinadoEm()), FONT_REGULAR, 6.8f, TEXTO_SECUNDARIO, textX, y - 20);
        desenharTexto(content, "E-mail: " + limitarPorLargura(valor(signatario.getEmail()), FONT_REGULAR, 6.6f, width - assinaturaWidth - 138), FONT_REGULAR, 6.6f, TEXTO_SECUNDARIO, textX, y - 31);

        String tokenHash = hashCurto(assinatura.getSolicitacaoAssinatura().getCodigoHash());
        desenharTexto(content, "Token/hash: " + tokenHash, FONT_REGULAR, 6.2f, TEXTO_SECUNDARIO, x + width - 116, y - 31);
    }

    private PDImageXObject carregarAssinaturaManuscrita(PDDocument pdf, Assinatura assinatura) {
        String dataUrl = extrairMetadado(assinatura.getMetadados(), "assinaturaManuscritaBase64");
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }

        int separador = dataUrl.indexOf(',');
        String base64 = separador >= 0 ? dataUrl.substring(separador + 1) : dataUrl;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return PDImageXObject.createFromByteArray(pdf, bytes, "assinatura-manuscrita-" + assinatura.getId());
        } catch (Exception exception) {
            return null;
        }
    }

    private String extrairMetadado(String metadados, String chave) {
        if (metadados == null || metadados.isBlank()) {
            return null;
        }

        String prefixo = chave + "=";
        for (String linha : metadados.split("\n")) {
            if (linha.startsWith(prefixo)) {
                return linha.substring(prefixo.length()).trim();
            }
        }

        return null;
    }

    private void desenharImagemProporcional(PDPageContentStream content, PDImageXObject image, float x, float y, float maxWidth, float maxHeight) throws Exception {
        float proporcao = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
        float width = image.getWidth() * proporcao;
        float height = image.getHeight() * proporcao;
        content.drawImage(image, x + ((maxWidth - width) / 2), y + ((maxHeight - height) / 2), width, height);
    }

    private void desenharCardEventos(PDPageContentStream content, Assinatura assinaturaAtual, List<SolicitacaoAssinatura> solicitacoes, List<Assinatura> assinaturas, float x, float y, float width, float height) throws Exception {
        desenharCard(content, x, y, width, height, "Eventos do documento");
        List<EventoManifesto> eventos = montarEventos(assinaturaAtual, solicitacoes, assinaturas);
        float eventoY = y + height - 42;
        int limite = Math.min(eventos.size(), 6);

        for (int index = 0; index < limite; index++) {
            EventoManifesto evento = eventos.get(index);
            desenharTexto(content, formatarData(evento.data()) + " - " + limitarPorLargura(evento.descricao(), FONT_REGULAR, 6.8f, width - 32), FONT_REGULAR, 6.8f, TEXTO, x + 16, eventoY);
            eventoY -= 18;
        }

        if (eventos.size() > limite) {
            desenharTexto(content, "+ " + (eventos.size() - limite) + " evento(s) adicional(is) mantido(s) nos logs de auditoria.", FONT_REGULAR, 6.4f, TEXTO_SECUNDARIO, x + 16, y + 13);
        }
    }

    private List<EventoManifesto> montarEventos(Assinatura assinaturaAtual, List<SolicitacaoAssinatura> solicitacoes, List<Assinatura> assinaturas) {
        Documento documento = assinaturaAtual.getSolicitacaoAssinatura().getDocumento();
        List<EventoManifesto> eventos = new ArrayList<>();

        eventos.add(new EventoManifesto(
                documento.getCriadoEm(),
                "Documento " + documento.getId() + " criado por " + valor(documento.getCriadoPorUsuario().getNome()) + "."
        ));

        for (SolicitacaoAssinatura solicitacao : solicitacoes) {
            eventos.add(new EventoManifesto(
                    solicitacao.getCriadoEm(),
                    "Lista de assinatura iniciada para " + valor(solicitacao.getSignatario().getEmail()) + "."
            ));
        }

        List<Assinatura> assinaturasEvento = assinaturas == null || assinaturas.isEmpty() ? List.of(assinaturaAtual) : assinaturas;
        for (Assinatura assinatura : assinaturasEvento) {
            Signatario signatario = assinatura.getSolicitacaoAssinatura().getSignatario();
            eventos.add(new EventoManifesto(
                    assinatura.getAssinadoEm() != null ? assinatura.getAssinadoEm() : assinatura.getCriadoEm(),
                    valor(signatario.getNome()) + " assinou - E-mail: " + valor(signatario.getEmail())
                            + " - IP: " + valorOuPadrao(assinatura.getIpAssinatura(), "Não informado")
                            + " - Token/hash: " + hashCurto(assinatura.getSolicitacaoAssinatura().getCodigoHash()) + "."
            ));
        }

        return eventos.stream()
                .sorted(Comparator.comparing(EventoManifesto::data, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void desenharRodapeManifesto(PDPageContentStream content, float x, float y, float width) throws Exception {
        content.setStrokingColor(BORDA);
        content.setLineWidth(0.4f);
        content.moveTo(x, y + 24);
        content.lineTo(x + width, y + 24);
        content.stroke();

        desenharTexto(content,
                "Manifesto emitido automaticamente pela Xsign para registrar a rastreabilidade da assinatura eletrônica.",
                FONT_REGULAR,
                6.8f,
                TEXTO_SECUNDARIO,
                x,
                y + 8);
    }

    private void desenharCard(PDPageContentStream content, float x, float y, float width, float height, String titulo) throws Exception {
        content.setNonStrokingColor(SUPERFICIE);
        content.addRect(x, y, width, height);
        content.fill();

        content.setStrokingColor(BORDA);
        content.setLineWidth(0.55f);
        content.addRect(x, y, width, height);
        content.stroke();

        content.setNonStrokingColor(AZUL_PRINCIPAL);
        content.addRect(x, y + height - 27, 3.2f, 16);
        content.fill();

        content.setStrokingColor(new Color(235, 241, 248));
        content.setLineWidth(0.35f);
        content.moveTo(x + 16, y + height - 30);
        content.lineTo(x + width - 16, y + height - 30);
        content.stroke();

        desenharTexto(content, titulo, FONT_BOLD, 8.6f, AZUL_MARINHO, x + 16, y + height - 18);
    }

    private void desenharLinhaInfo(PDPageContentStream content, String rotulo, String valor, float x, float y, float larguraMaxima) throws Exception {
        desenharTexto(content, rotulo.toUpperCase(), FONT_BOLD, 5.5f, TEXTO_SECUNDARIO, x, y + 9);
        desenharTexto(content, limitarPorLargura(valor, FONT_REGULAR, 7.1f, larguraMaxima), FONT_REGULAR, 7.1f, TEXTO, x, y);
    }

    private void desenharTexto(PDPageContentStream content, String texto, PDType1Font fonte, float tamanho, Color cor, float x, float y) throws Exception {
        content.beginText();
        content.setNonStrokingColor(cor);
        content.setFont(fonte, tamanho);
        content.newLineAtOffset(x, y);
        content.showText(textoPdf(texto));
        content.endText();
    }

    private void desenharTextoRotacionado(PDPageContentStream content, String texto, PDType1Font fonte, float tamanho, Color cor, float x, float y, double radians) throws Exception {
        content.beginText();
        content.setNonStrokingColor(cor);
        content.setFont(fonte, tamanho);
        content.setTextMatrix(Matrix.getRotateInstance(radians, x, y));
        content.showText(textoPdf(texto));
        content.endText();
    }

    private void registrarCertificado(UUID certificadoId, Documento documento, Assinatura assinatura, ArquivoUploadResultado upload, String urlVerificacao) {
        Certificado certificado = new Certificado();
        certificado.setId(certificadoId);
        certificado.setDocumento(documento);
        certificado.setAssinatura(assinatura);
        certificado.setSignatarioFinal(assinatura.getSolicitacaoAssinatura().getSignatario());
        certificado.setHashDocumento(assinatura.getDocumentoHashSha256());
        certificado.setAssinaturaDigital(montarAssinaturaDigital(assinatura));
        certificado.setDadosAutenticacao(montarDadosAutenticacao(certificadoId, assinatura));
        certificado.setUrlDocumento(storageProvider.getUrlOuCaminho(upload.caminho()));
        certificado.setUrlVerificacao(urlVerificacao);
        certificadoRepository.saveAndFlush(certificado);
    }

    private ArquivoArmazenado buscarArquivoOriginal(Documento documento) {
        List<DocumentoVersao> versoes = documentoVersaoRepository.findAllByDocumentoIdOrderByNumeroVersaoAsc(documento.getId());
        if (versoes.isEmpty()) {
            return documento.getArquivoAtual();
        }

        return versoes.get(0).getArquivo();
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

    private void registrarNovaVersao(Documento documento, ArquivoArmazenado arquivoAssinado) {
        DocumentoVersao ultimaVersao = documentoVersaoRepository.findTopByDocumentoIdOrderByNumeroVersaoDesc(documento.getId());

        DocumentoVersao versao = new DocumentoVersao();
        versao.setDocumento(documento);
        versao.setArquivo(arquivoAssinado);
        versao.setNumeroVersao(ultimaVersao == null ? 1 : ultimaVersao.getNumeroVersao() + 1);
        versao.setCriadoPorUsuario(documento.getCriadoPorUsuario());
        documentoVersaoRepository.saveAndFlush(versao);
    }

    private String resolverNomeAssinado(String nomeOriginal) {
        String nome = nomeOriginal == null || nomeOriginal.isBlank() ? "documento.pdf" : nomeOriginal;
        int index = nome.toLowerCase().lastIndexOf(".pdf");
        if (index < 0) {
            return nome + "-assinado.pdf";
        }

        return nome.substring(0, index) + "-assinado.pdf";
    }

    private String montarUrlVerificacao(UUID certificadoId) {
        String baseUrl = baseUrlVerificacao == null || baseUrlVerificacao.isBlank()
                ? "http://localhost:5173/verificar/certificado"
                : baseUrlVerificacao.trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + certificadoId;
        }

        return baseUrl + "/" + certificadoId;
    }

    private String montarAssinaturaDigital(Assinatura assinatura) {
        return "Xsign-CortexSign-v1:"
                + valor(assinatura.getProtocolo())
                + ":"
                + valor(assinatura.getEvidenciaHashSha256());
    }

    private String montarDadosAutenticacao(UUID certificadoId, Assinatura assinatura) {
        SolicitacaoAssinatura solicitacao = assinatura.getSolicitacaoAssinatura();
        Signatario signatario = solicitacao.getSignatario();
        return String.join("\n",
                "certificadoId=" + certificadoId,
                "documentoId=" + solicitacao.getDocumento().getId(),
                "assinaturaId=" + assinatura.getId(),
                "solicitacaoAssinaturaId=" + solicitacao.getId(),
                "signatarioId=" + signatario.getId(),
                "signatarioEmail=" + valor(signatario.getEmail()),
                "protocolo=" + valor(assinatura.getProtocolo()),
                "ip=" + valorOuPadrao(assinatura.getIpAssinatura(), "Não informado"),
                "codigoHash=" + valor(solicitacao.getCodigoHash()),
                "tokenHash=" + gerarHashSha256(solicitacao.getToken()),
                "documentoHashSha256=" + valor(assinatura.getDocumentoHashSha256()),
                "evidenciaHashSha256=" + valor(assinatura.getEvidenciaHashSha256()),
                "arquivoAssinadoHashSha256=" + valor(assinatura.getSolicitacaoAssinatura().getDocumento().getArquivoAtual().getChecksumSha256()),
                "assinadoEm=" + formatarData(assinatura.getAssinadoEm())
        );
    }

    private String gerarHashSha256(String valor) {
        if (valor == null) {
            return "-";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar hash SHA-256", exception);
        }
    }

    private String formatarData(LocalDateTime data) {
        if (data == null) {
            return "-";
        }

        return data.format(DATA_FORMATTER);
    }

    private String formatarDataComFuso(LocalDateTime data) {
        return formatarData(data) + " (UTC-0300)";
    }

    private String hashCurto(String hash) {
        if (hash == null || hash.isBlank()) {
            return "-";
        }

        if (hash.length() <= 20) {
            return hash;
        }

        return hash.substring(0, 10) + "..." + hash.substring(hash.length() - 10);
    }

    private String valor(Object valor) {
        if (valor == null) {
            return "-";
        }

        return valor.toString();
    }

    private String valorOuPadrao(String valor, String padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }

        return valor;
    }

    private float larguraTexto(PDType1Font fonte, String texto, float tamanho) throws Exception {
        return fonte.getStringWidth(textoPdf(texto)) / 1000f * tamanho;
    }

    private String limitarPorLargura(String valor, PDType1Font fonte, float tamanho, float larguraMaxima) throws Exception {
        String texto = textoPdf(valor == null ? "-" : valor);
        if (larguraTexto(fonte, texto, tamanho) <= larguraMaxima) {
            return texto;
        }

        String sufixo = "...";
        while (!texto.isEmpty() && larguraTexto(fonte, texto + sufixo, tamanho) > larguraMaxima) {
            texto = texto.substring(0, texto.length() - 1);
        }

        return texto.isEmpty() ? sufixo : texto + sufixo;
    }

    private String textoPdf(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace('–', '-')
                .replace('—', '-')
                .replace('‘', '\'')
                .replace('’', '\'')
                .replace('“', '"')
                .replace('”', '"')
                .replace('•', '-')
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("[^\\u0020-\\u00FF]", " ");
    }

    private record EventoManifesto(LocalDateTime data, String descricao) {
    }
}
