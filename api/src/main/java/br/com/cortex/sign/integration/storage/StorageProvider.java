package br.com.cortex.sign.integration.storage;

import br.com.cortex.sign.integration.storage.dto.ArquivoUploadResultado;
import br.com.cortex.sign.integration.storage.enums.TipoProvedorStorage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageProvider {

    ArquivoUploadResultado upload(MultipartFile arquivo, String pastaDestino);

    ArquivoUploadResultado upload(byte[] conteudo, String nomeOriginal, String tipoConteudo, String pastaDestino);

    Resource download(String caminho);

    void delete(String caminho);

    String getUrlOuCaminho(String caminho);

    TipoProvedorStorage getProviderType();
}
