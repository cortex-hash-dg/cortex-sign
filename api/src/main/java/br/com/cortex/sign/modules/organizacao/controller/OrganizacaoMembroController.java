package br.com.cortex.sign.modules.organizacao.controller;

import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.dto.request.AdicionarMembroOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarMembroOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoMembroResponse;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoMembroService;
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
@RequestMapping("/api/organizacoes/{organizacaoId}/membros")
@RequiredArgsConstructor
public class OrganizacaoMembroController {

    private final OrganizacaoMembroService membroService;

    @GetMapping
    public List<OrganizacaoMembroResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID organizacaoId
    ) {
        return membroService.listarPorOrganizacao(usuarioAutenticado, organizacaoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoMembroResponse adicionar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID organizacaoId,
            @Valid @RequestBody AdicionarMembroOrganizacaoRequest request
    ) {
        return membroService.adicionar(usuarioAutenticado, organizacaoId, request);
    }

    @PutMapping("/{membroId}")
    public OrganizacaoMembroResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID organizacaoId,
            @PathVariable UUID membroId,
            @Valid @RequestBody AtualizarMembroOrganizacaoRequest request
    ) {
        return membroService.atualizar(usuarioAutenticado, organizacaoId, membroId, request);
    }

    @DeleteMapping("/{membroId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @PathVariable UUID organizacaoId,
            @PathVariable UUID membroId
    ) {
        membroService.remover(usuarioAutenticado, organizacaoId, membroId);
    }
}
