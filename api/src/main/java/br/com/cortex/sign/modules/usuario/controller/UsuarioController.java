package br.com.cortex.sign.modules.usuario.controller;

import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.usuario.dto.request.AtualizarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.request.CriarUsuarioRequest;
import br.com.cortex.sign.modules.usuario.dto.response.UsuarioResponse;
import br.com.cortex.sign.modules.usuario.service.UsuarioService;
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
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody CriarUsuarioRequest request
    ) {
        return usuarioService.criar(usuarioAutenticado, request);
    }

    @GetMapping
    public List<UsuarioResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        return usuarioService.listar(usuarioAutenticado);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        return usuarioService.buscarPorId(usuarioAutenticado, id);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        return usuarioService.atualizar(usuarioAutenticado, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        usuarioService.excluir(usuarioAutenticado, id);
    }
}
