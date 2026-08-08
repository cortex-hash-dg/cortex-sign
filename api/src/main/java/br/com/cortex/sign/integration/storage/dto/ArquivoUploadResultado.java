package br.com.cortex.sign.integration.storage.dto;

import br.com.cortex.sign.integration.storage.enums.TipoProvedorStorage;

public record ArquivoUploadResultado(
        TipoProvedorStorage provedor,
        String nomeOriginal,
        String nomeArmazenado,
        String caminho,
        String tipoConteudo,
        Long tamanhoBytes,
        String checksumSha256
) {
}
