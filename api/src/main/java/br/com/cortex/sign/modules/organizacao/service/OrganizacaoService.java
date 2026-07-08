package br.com.cortex.sign.modules.organizacao.service;

import br.com.cortex.sign.common.exception.RecursoNaoEncontradoException;
import br.com.cortex.sign.modules.organizacao.dto.request.AtualizarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.request.CriarOrganizacaoRequest;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.mapper.OrganizacaoMapper;
import br.com.cortex.sign.modules.organizacao.repository.OrganizacaoRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final OrganizacaoMapper organizacaoMapper;

    @Transactional
    public OrganizacaoResponse criar(CriarOrganizacaoRequest request) {
        Organizacao organizacao = organizacaoMapper.toEntity(request);
        Organizacao organizacaoSalva = organizacaoRepository.saveAndFlush(organizacao);

        return organizacaoMapper.toResponse(organizacaoSalva);
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoResponse> listar() {
        return organizacaoRepository.findAll()
                .stream()
                .map(organizacaoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizacaoResponse buscarPorId(UUID id) {
        return organizacaoMapper.toResponse(buscarEntidadePorId(id));
    }

    @Transactional
    public OrganizacaoResponse atualizar(UUID id, AtualizarOrganizacaoRequest request) {
        Organizacao organizacao = buscarEntidadePorId(id);

        organizacaoMapper.updateEntity(organizacao, request);

        Organizacao organizacaoAtualizada = organizacaoRepository.saveAndFlush(organizacao);

        return organizacaoMapper.toResponse(organizacaoAtualizada);
    }

    @Transactional
    public void excluir(UUID id) {
        Organizacao organizacao = buscarEntidadePorId(id);

        organizacaoRepository.delete(organizacao);
    }

    public Organizacao buscarEntidadePorId(UUID id) {
        return organizacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Organização não encontrada"));
    }
}
