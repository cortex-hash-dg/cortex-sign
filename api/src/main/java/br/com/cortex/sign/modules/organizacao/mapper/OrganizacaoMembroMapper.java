package br.com.cortex.sign.modules.organizacao.mapper;

import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoMembroResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.entity.OrganizacaoMembro;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class OrganizacaoMembroMapper {

    public OrganizacaoMembroResponse toResponse(OrganizacaoMembro membro) {
        Usuario usuario = membro.getUsuario();
        Organizacao organizacao = membro.getOrganizacao();

        return new OrganizacaoMembroResponse(
                membro.getId(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                organizacao.getId(),
                organizacao.getNome(),
                membro.getPapel(),
                membro.getStatus(),
                membro.getConvidadoEm(),
                membro.getAceitoEm(),
                membro.getSuspensoEm(),
                membro.getRemovidoEm(),
                membro.getCriadoEm(),
                membro.getAtualizadoEm()
        );
    }
}
