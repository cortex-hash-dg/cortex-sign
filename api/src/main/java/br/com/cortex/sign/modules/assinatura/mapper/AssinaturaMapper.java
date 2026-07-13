package br.com.cortex.sign.modules.assinatura.mapper;

import br.com.cortex.sign.modules.assinatura.dto.response.AssinaturaPublicaResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.AssinaturaResponse;
import br.com.cortex.sign.modules.assinatura.dto.response.SolicitacaoAssinaturaResponse;
import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.assinatura.entity.SolicitacaoAssinatura;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import org.springframework.stereotype.Component;

@Component
public class AssinaturaMapper {

    public SolicitacaoAssinaturaResponse toResponse(SolicitacaoAssinatura solicitacao, Assinatura assinatura, String codigoTeste) {
        Documento documento = solicitacao.getDocumento();
        Signatario signatario = solicitacao.getSignatario();

        return new SolicitacaoAssinaturaResponse(
                solicitacao.getId(),
                documento.getId(),
                documento.getTitulo(),
                signatario.getId(),
                signatario.getNome(),
                signatario.getEmail(),
                solicitacao.getStatus(),
                solicitacao.getToken(),
                codigoTeste,
                solicitacao.getExpiraEm(),
                assinatura != null ? toResponse(assinatura) : null,
                solicitacao.getCriadoEm(),
                solicitacao.getAtualizadoEm()
        );
    }

    public AssinaturaResponse toResponse(Assinatura assinatura) {
        return new AssinaturaResponse(
                assinatura.getId(),
                assinatura.getSolicitacaoAssinatura().getId(),
                assinatura.getTipo(),
                assinatura.getStatus(),
                assinatura.getProvedor(),
                assinatura.getProtocolo(),
                assinatura.getAssinadoEm(),
                assinatura.getRejeitadoEm(),
                assinatura.getMotivoRejeicao(),
                assinatura.getMetadados(),
                assinatura.getCriadoEm()
        );
    }

    public AssinaturaPublicaResponse toPublicResponse(SolicitacaoAssinatura solicitacao) {
        Documento documento = solicitacao.getDocumento();
        Signatario signatario = solicitacao.getSignatario();

        return new AssinaturaPublicaResponse(
                solicitacao.getId(),
                documento.getId(),
                documento.getTitulo(),
                signatario.getNome(),
                signatario.getEmail(),
                solicitacao.getStatus(),
                solicitacao.getExpiraEm()
        );
    }
}
