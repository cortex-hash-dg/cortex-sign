package br.com.cortex.sign.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cortex.sign.dto.AtualizarOrganizacaoRequest;
import br.com.cortex.sign.dto.CriarOrganizacaoRequest;
import br.com.cortex.sign.dto.OrganizacaoResponse;
import br.com.cortex.sign.entity.Organizacao;
import br.com.cortex.sign.exception.RecursoNaoEncontradoException;
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

    @Transactional(readOnly = true)
    public OrganizacaoResponse buscarPorId(UUID id) {
        Organizacao organizacao = buscarEntidadePorId(id);

        return toResponse(organizacao);
    }

    @Transactional
    public OrganizacaoResponse atualizar(UUID id, AtualizarOrganizacaoRequest request) {
        Organizacao organizacao = buscarEntidadePorId(id);

        organizacao.setNome(request.nome());
        organizacao.setNumeroDocumento(request.numeroDocumento());

        Organizacao organizacaoAtualizada = organizacaoRepository.saveAndFlush(organizacao);

        return toResponse(organizacaoAtualizada);
    }

    @Transactional
    public void excluir(UUID id) {
        Organizacao organizacao = buscarEntidadePorId(id);

        organizacaoRepository.delete(organizacao);
    }

    private Organizacao buscarEntidadePorId(UUID id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada"));
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