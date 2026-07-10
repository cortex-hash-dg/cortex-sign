package br.com.cortex.sign.modules.documento.dto.response;

import org.springframework.core.io.Resource;

public record DocumentoDownloadResponse(
        Resource resource,
        String nomeArquivo,
        String tipoConteudo,
        Long tamanhoBytes
) {
}
