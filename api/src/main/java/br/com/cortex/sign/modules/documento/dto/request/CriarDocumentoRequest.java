package br.com.cortex.sign.modules.documento.dto.request;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record CriarDocumentoRequest(
        UUID organizacaoId,
        String titulo,
        MultipartFile arquivo
) {
}
