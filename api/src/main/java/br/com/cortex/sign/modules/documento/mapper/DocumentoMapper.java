package br.com.cortex.sign.modules.documento.mapper;

import br.com.cortex.sign.modules.documento.dto.response.ArquivoDocumentoResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoDetalheResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoResumoResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoVersaoResponse;
import br.com.cortex.sign.modules.documento.entity.ArquivoArmazenado;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.documento.entity.DocumentoVersao;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentoMapper {

    public DocumentoResumoResponse toResumoResponse(Documento documento) {
        Organizacao organizacao = documento.getOrganizacao();
        Usuario criadoPorUsuario = documento.getCriadoPorUsuario();

        return new DocumentoResumoResponse(
                documento.getId(),
                organizacao.getId(),
                organizacao.getNome(),
                documento.getTitulo(),
                documento.getStatus(),
                criadoPorUsuario.getId(),
                criadoPorUsuario.getNome(),
                documento.getCriadoEm(),
                documento.getAtualizadoEm()
        );
    }

    public DocumentoDetalheResponse toDetalheResponse(Documento documento, List<DocumentoVersao> versoes) {
        Organizacao organizacao = documento.getOrganizacao();
        Usuario criadoPorUsuario = documento.getCriadoPorUsuario();

        return new DocumentoDetalheResponse(
                documento.getId(),
                organizacao.getId(),
                organizacao.getNome(),
                documento.getTitulo(),
                documento.getStatus(),
                criadoPorUsuario.getId(),
                criadoPorUsuario.getNome(),
                toArquivoResponse(documento.getArquivoAtual()),
                versoes.stream().map(this::toVersaoResponse).toList(),
                documento.getCriadoEm(),
                documento.getAtualizadoEm()
        );
    }

    private DocumentoVersaoResponse toVersaoResponse(DocumentoVersao versao) {
        Usuario criadoPorUsuario = versao.getCriadoPorUsuario();

        return new DocumentoVersaoResponse(
                versao.getId(),
                versao.getNumeroVersao(),
                toArquivoResponse(versao.getArquivo()),
                criadoPorUsuario.getId(),
                criadoPorUsuario.getNome(),
                versao.getCriadoEm()
        );
    }

    private ArquivoDocumentoResponse toArquivoResponse(ArquivoArmazenado arquivo) {
        return new ArquivoDocumentoResponse(
                arquivo.getId(),
                arquivo.getProvedor(),
                arquivo.getNomeOriginal(),
                arquivo.getNomeArmazenado(),
                arquivo.getCaminho(),
                arquivo.getTipoConteudo(),
                arquivo.getTamanhoBytes(),
                arquivo.getChecksumSha256(),
                arquivo.getCriadoEm()
        );
    }
}
