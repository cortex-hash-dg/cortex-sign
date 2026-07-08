package br.com.cortex.sign.modules.organizacao.mapper;

import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.CriarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import org.springframework.stereotype.Component;

@Component
public class OrganizacaoMapper {

    public Organizacao toEntity(CriarOrganizacaoRequest request) {
        Organizacao organizacao = new Organizacao();
        organizacao.setNome(request.nome());
        organizacao.setNumeroDocumento(request.numeroDocumento());
        return organizacao;
    }

    public void updateEntity(Organizacao organizacao, AtualizarOrganizacaoRequest request) {
        organizacao.setNome(request.nome());
        organizacao.setNumeroDocumento(request.numeroDocumento());
    }

    public OrganizacaoResponse toResponse(Organizacao organizacao) {
        return new OrganizacaoResponse(
                organizacao.getId(),
                organizacao.getNome(),
                organizacao.getNumeroDocumento(),
                organizacao.getCriadoEm(),
                organizacao.getAtualizadoEm()
        );
    }
}
