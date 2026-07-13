package br.com.cortex.sign.modules.signatario.controller;

import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.signatario.dto.request.AtualizarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.request.CriarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.response.SignatarioResponse;
import br.com.cortex.sign.modules.signatario.service.SignatarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SignatarioController {

    private final SignatarioService signatarioService;

    @PostMapping("/api/documentos/{documentoId}/signatarios")
    @ResponseStatus(HttpStatus.CREATED)
    public SignatarioResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID documentoId,
            @Valid @RequestBody CriarSignatarioRequest request
    ) {
        return signatarioService.criar(usuarioAutenticado, documentoId, request);
    }

    @GetMapping("/api/documentos/{documentoId}/signatarios")
    public List<SignatarioResponse> listarPorDocumento(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID documentoId
    ) {
        return signatarioService.listarPorDocumento(usuarioAutenticado, documentoId);
    }

    @GetMapping("/api/signatarios/{id}")
    public SignatarioResponse buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        return signatarioService.buscarPorId(usuarioAutenticado, id);
    }

    @PutMapping("/api/signatarios/{id}")
    public SignatarioResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarSignatarioRequest request
    ) {
        return signatarioService.atualizar(usuarioAutenticado, id, request);
    }

    @DeleteMapping("/api/signatarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        signatarioService.excluir(usuarioAutenticado, id);
    }
}
