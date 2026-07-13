package br.com.cortex.sign.modules.auditoria.controller;

import br.com.cortex.sign.modules.auditoria.dto.response.LogAuditoriaResponse;
import br.com.cortex.sign.modules.auditoria.service.AuditoriaService;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public List<LogAuditoriaResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado usuarioAutenticado,
            @RequestParam(value = "organizacaoId", required = false) UUID organizacaoId
    ) {
        return auditoriaService.listar(usuarioAutenticado, organizacaoId);
    }
}
