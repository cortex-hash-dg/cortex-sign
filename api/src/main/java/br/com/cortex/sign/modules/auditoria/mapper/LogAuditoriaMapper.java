package br.com.cortex.sign.modules.auditoria.mapper;

import br.com.cortex.sign.modules.auditoria.dto.response.LogAuditoriaResponse;
import br.com.cortex.sign.modules.auditoria.entity.LogAuditoria;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class LogAuditoriaMapper {

    public LogAuditoriaResponse toResponse(LogAuditoria log) {
        Organizacao organizacao = log.getOrganizacao();
        Usuario usuario = log.getUsuario();

        return new LogAuditoriaResponse(
                log.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario != null ? usuario.getId() : null,
                usuario != null ? usuario.getNome() : null,
                log.getAcao(),
                log.getEntidadeTipo(),
                log.getEntidadeId(),
                log.getDetalhes(),
                log.getCriadoEm()
        );
    }
}
