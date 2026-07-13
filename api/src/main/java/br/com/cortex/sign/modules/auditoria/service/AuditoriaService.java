package br.com.cortex.sign.modules.auditoria.service;

import br.com.cortex.sign.common.exception.AcessoNegadoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.modules.auditoria.dto.response.LogAuditoriaResponse;
import br.com.cortex.sign.modules.auditoria.entity.LogAuditoria;
import br.com.cortex.sign.modules.auditoria.mapper.LogAuditoriaMapper;
import br.com.cortex.sign.modules.auditoria.repository.LogAuditoriaRepository;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoService;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoService organizacaoService;
    private final LogAuditoriaMapper logAuditoriaMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Organizacao organizacao, Usuario usuario, String acao, String entidadeTipo, UUID entidadeId, String detalhes) {
        LogAuditoria log = new LogAuditoria();
        log.setOrganizacao(organizacao);
        log.setUsuario(usuario);
        log.setAcao(acao);
        log.setEntidadeTipo(entidadeTipo);
        log.setEntidadeId(entidadeId);
        log.setDetalhes(detalhes);

        logAuditoriaRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<LogAuditoriaResponse> listar(UsuarioAutenticado usuarioAutenticado, UUID organizacaoId) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);

        if (usuario.getPerfil() == PerfilUsuario.SUPER_ADMINISTRADOR) {
            if (organizacaoId != null) {
                organizacaoService.buscarEntidadePorId(organizacaoId);
                return logAuditoriaRepository.findTop100ByOrganizacaoIdOrderByCriadoEmDesc(organizacaoId)
                        .stream()
                        .map(logAuditoriaMapper::toResponse)
                        .toList();
            }

            return logAuditoriaRepository.findTop100ByOrderByCriadoEmDesc()
                    .stream()
                    .map(logAuditoriaMapper::toResponse)
                    .toList();
        }

        if (usuario.getPerfil() != PerfilUsuario.ADMINISTRADOR_ORGANIZACAO
                && usuario.getPerfil() != PerfilUsuario.AUDITOR) {
            throw new AcessoNegadoException("Você não tem permissão para consultar auditoria");
        }

        if (usuario.getOrganizacao() == null) {
            throw new AcessoNegadoException("Usuário não está vinculado a uma organização");
        }

        return logAuditoriaRepository.findTop100ByOrganizacaoIdOrderByCriadoEmDesc(usuario.getOrganizacao().getId())
                .stream()
                .map(logAuditoriaMapper::toResponse)
                .toList();
    }

    private Usuario buscarUsuarioAutenticado(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }

        return usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));
    }
}
