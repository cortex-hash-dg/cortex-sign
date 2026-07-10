package br.com.cortex.sign.modules.documento.controller;

import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.documento.dto.request.AtualizarDocumentoRequest;
import br.com.cortex.sign.modules.documento.dto.request.CriarDocumentoRequest;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoDetalheResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoDownloadResponse;
import br.com.cortex.sign.modules.documento.dto.response.DocumentoResumoResponse;
import br.com.cortex.sign.modules.documento.service.DocumentoService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDetalheResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @RequestParam(value = "organizacaoId", required = false) UUID organizacaoId,
            @RequestParam(value = "titulo", required = false) String titulo,
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        return documentoService.criar(
                usuarioAutenticado,
                new CriarDocumentoRequest(organizacaoId, titulo, arquivo)
        );
    }

    @GetMapping
    public List<DocumentoResumoResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @RequestParam(value = "organizacaoId", required = false) UUID organizacaoId
    ) {
        return documentoService.listar(usuarioAutenticado, organizacaoId);
    }

    @GetMapping("/{id}")
    public DocumentoDetalheResponse buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        return documentoService.buscarPorId(usuarioAutenticado, id);
    }

    @PutMapping("/{id}")
    public DocumentoDetalheResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarDocumentoRequest request
    ) {
        return documentoService.atualizar(usuarioAutenticado, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void arquivar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        documentoService.arquivar(usuarioAutenticado, id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> baixar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        DocumentoDownloadResponse download = documentoService.baixar(usuarioAutenticado, id);

        return ResponseEntity.ok()
                .contentType(resolverTipoConteudo(download.tipoConteudo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.nomeArquivo(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentLength(download.tamanhoBytes())
                .body(download.resource());
    }

    private MediaType resolverTipoConteudo(String tipoConteudo) {
        if (tipoConteudo == null || tipoConteudo.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(tipoConteudo);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
