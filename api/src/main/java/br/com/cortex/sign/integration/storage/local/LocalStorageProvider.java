package br.com.cortex.sign.integration.storage.local;

import br.com.cortex.sign.common.exception.ArmazenamentoException;
import br.com.cortex.sign.integration.storage.StorageProvider;
import br.com.cortex.sign.integration.storage.dto.ArquivoUploadResultado;
import br.com.cortex.sign.integration.storage.enums.TipoProvedorStorage;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalStorageProvider implements StorageProvider {

    @Value("${app.storage.local.base-path:./storage}")
    private String basePathConfig;

    private Path basePath;

    @PostConstruct
    void init() {
        try {
            basePath = Paths.get(basePathConfig).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
        } catch (Exception exception) {
            throw new ArmazenamentoException("Não foi possível inicializar o armazenamento local", exception);
        }
    }

    @Override
    public ArquivoUploadResultado upload(MultipartFile arquivo, String pastaDestino) {
        try {
            String nomeOriginal = limparNomeOriginal(arquivo.getOriginalFilename());
            String extensao = obterExtensao(nomeOriginal);
            String nomeArmazenado = UUID.randomUUID() + extensao;

            Path pasta = resolverCaminhoSeguro(pastaDestino);
            Files.createDirectories(pasta);

            Path destino = pasta.resolve(nomeArmazenado).normalize();
            if (!destino.startsWith(basePath)) {
                throw new ArmazenamentoException("Caminho de armazenamento inválido");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = arquivo.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                Files.copy(digestInputStream, destino);
            }

            String caminhoRelativo = basePath.relativize(destino).toString().replace('\\', '/');
            String checksum = HexFormat.of().formatHex(digest.digest());

            return new ArquivoUploadResultado(
                    getProviderType(),
                    nomeOriginal,
                    nomeArmazenado,
                    caminhoRelativo,
                    arquivo.getContentType(),
                    arquivo.getSize(),
                    checksum
            );
        } catch (ArmazenamentoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArmazenamentoException("Não foi possível armazenar o arquivo", exception);
        }
    }

    @Override
    public ArquivoUploadResultado upload(byte[] conteudo, String nomeOriginal, String tipoConteudo, String pastaDestino) {
        try {
            String nomeSeguro = limparNomeOriginal(nomeOriginal);
            String extensao = obterExtensao(nomeSeguro);
            String nomeArmazenado = UUID.randomUUID() + extensao;

            Path pasta = resolverCaminhoSeguro(pastaDestino);
            Files.createDirectories(pasta);

            Path destino = pasta.resolve(nomeArmazenado).normalize();
            if (!destino.startsWith(basePath)) {
                throw new ArmazenamentoException("Caminho de armazenamento inválido");
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = new ByteArrayInputStream(conteudo);
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                Files.copy(digestInputStream, destino);
            }

            String caminhoRelativo = basePath.relativize(destino).toString().replace('\\', '/');
            String checksum = HexFormat.of().formatHex(digest.digest());

            return new ArquivoUploadResultado(
                    getProviderType(),
                    nomeSeguro,
                    nomeArmazenado,
                    caminhoRelativo,
                    tipoConteudo,
                    (long) conteudo.length,
                    checksum
            );
        } catch (ArmazenamentoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArmazenamentoException("Não foi possível armazenar o arquivo", exception);
        }
    }

    @Override
    public Resource download(String caminho) {
        try {
            Path arquivo = resolverCaminhoSeguro(caminho);
            if (!Files.exists(arquivo) || !Files.isRegularFile(arquivo)) {
                throw new ArmazenamentoException("Arquivo não encontrado no armazenamento");
            }

            return new UrlResource(arquivo.toUri());
        } catch (ArmazenamentoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArmazenamentoException("Não foi possível baixar o arquivo", exception);
        }
    }

    @Override
    public void delete(String caminho) {
        try {
            Files.deleteIfExists(resolverCaminhoSeguro(caminho));
        } catch (Exception exception) {
            throw new ArmazenamentoException("Não foi possível excluir o arquivo", exception);
        }
    }

    @Override
    public String getUrlOuCaminho(String caminho) {
        return resolverCaminhoSeguro(caminho).toString();
    }

    @Override
    public TipoProvedorStorage getProviderType() {
        return TipoProvedorStorage.LOCAL;
    }

    private Path resolverCaminhoSeguro(String caminho) {
        Path resolvido = basePath.resolve(caminho).normalize();
        if (!resolvido.startsWith(basePath)) {
            throw new ArmazenamentoException("Caminho de armazenamento inválido");
        }

        return resolvido;
    }

    private String limparNomeOriginal(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "documento.pdf";
        }

        return Paths.get(nomeOriginal).getFileName().toString();
    }

    private String obterExtensao(String nomeOriginal) {
        int index = nomeOriginal.lastIndexOf('.');
        if (index < 0) {
            return ".pdf";
        }

        return nomeOriginal.substring(index).toLowerCase();
    }
}
