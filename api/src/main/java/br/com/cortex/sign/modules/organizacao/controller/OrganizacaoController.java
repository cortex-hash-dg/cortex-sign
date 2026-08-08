package br.com.cortex.sign.modules.organizacao.controller;

import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.CriarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoResponse;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoService;
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
@RequestMapping("/api/organizacoes")
@RequiredArgsConstructor
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @Valid @RequestBody CriarOrganizacaoRequest request
    ) {
        return organizacaoService.criar(usuarioAutenticado, request);
    }

    @GetMapping
    public List<OrganizacaoResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado) {
        return organizacaoService.listar(usuarioAutenticado);
    }

    @GetMapping("/{id}")
    public OrganizacaoResponse buscarPorId(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        return organizacaoService.buscarPorId(usuarioAutenticado, id);
    }

    @PutMapping("/{id}")
    public OrganizacaoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarOrganizacaoRequest request
    ) {
        return organizacaoService.atualizar(usuarioAutenticado, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID id
    ) {
        organizacaoService.excluir(usuarioAutenticado, id);
    }
}
