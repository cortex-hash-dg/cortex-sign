package br.com.cortex.sign.modules.signatario.mapper;

import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.signatario.dto.request.AtualizarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.request.CriarSignatarioRequest;
import br.com.cortex.sign.modules.signatario.dto.response.SignatarioResponse;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import org.springframework.stereotype.Component;

@Component
public class SignatarioMapper {

    public Signatario toEntity(CriarSignatarioRequest request, Documento documento) {
        Signatario signatario = new Signatario();
        signatario.setDocumento(documento);
        signatario.setNome(request.nome().trim());
        signatario.setEmail(request.email().trim().toLowerCase());
        signatario.setNumeroDocumento(normalizarOpcional(request.numeroDocumento()));
        signatario.setTelefone(normalizarOpcional(request.telefone()));
        signatario.setTipo(request.tipo());
        signatario.setOrdemAssinatura(request.ordemAssinatura() != null ? request.ordemAssinatura() : 1);
        return signatario;
    }

    public void updateEntity(Signatario signatario, AtualizarSignatarioRequest request) {
        signatario.setNome(request.nome().trim());
        signatario.setEmail(request.email().trim().toLowerCase());
        signatario.setNumeroDocumento(normalizarOpcional(request.numeroDocumento()));
        signatario.setTelefone(normalizarOpcional(request.telefone()));
        signatario.setTipo(request.tipo());
        signatario.setOrdemAssinatura(request.ordemAssinatura() != null ? request.ordemAssinatura() : 1);
    }

    public SignatarioResponse toResponse(Signatario signatario) {
        Documento documento = signatario.getDocumento();

        return new SignatarioResponse(
                signatario.getId(),
                documento.getId(),
                documento.getTitulo(),
                signatario.getNome(),
                signatario.getEmail(),
                signatario.getNumeroDocumento(),
                signatario.getTelefone(),
                signatario.getTipo(),
                signatario.getOrdemAssinatura(),
                signatario.getCriadoEm(),
                signatario.getAtualizadoEm()
        );
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
