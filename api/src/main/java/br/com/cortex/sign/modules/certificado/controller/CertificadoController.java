package br.com.cortex.sign.modules.certificado.controller;

import br.com.cortex.sign.modules.certificado.dto.response.CertificadoDocumentoValidacaoResponse;
import br.com.cortex.sign.modules.certificado.dto.response.CertificadoValidacaoPublicaResponse;
import br.com.cortex.sign.modules.certificado.service.CertificadoService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    @GetMapping("/verificar/{id}")
    public CertificadoValidacaoPublicaResponse verificar(@PathVariable UUID id) {
        return certificadoService.verificar(id);
    }

    @PostMapping(value = "/verificar/documento", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CertificadoDocumentoValidacaoResponse verificarDocumento(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "certificadoId", required = false) UUID certificadoId
    ) {
        return certificadoService.verificarDocumento(arquivo, certificadoId);
    }
}
