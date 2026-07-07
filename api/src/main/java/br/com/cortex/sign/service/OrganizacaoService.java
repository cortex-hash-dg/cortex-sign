package br.com.cortex.sign.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cortex.sign.dto.CriarOrganizacaoRequest;
import br.com.cortex.sign.dto.OrganizacaoResponse;
import br.com.cortex.sign.entity.Organizacao;
import br.com.cortex.sign.repository.OrganizacaoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;

    @Transactional
    public OrganizacaoResponse criar(CriarOrganizacaoRequest request) {
        Organizacao organizacao = new Organizacao();
        organizacao.setNome(request.nome());
        organizacao.setNumeroDocumento(request.numeroDocumento());

        Organizacao organizacaoSalva = organizacaoRepository.saveAndFlush(organizacao);

        return toResponse(organizacaoSalva);
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponse> listar() {
        return organizacaoRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private OrganizacaoResponse toResponse(Organizacao organizacao) {
        return new OrganizacaoResponse(
                organizacao.getId(),
                organizacao.getNome(),
                organizacao.getNumeroDocumento(),
                organizacao.getCriadoEm(),
                organizacao.getAtualizadoEm()
        );
    }
}