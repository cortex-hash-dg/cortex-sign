package br.com.cortex.sign.integration.assinatura.mock;

import br.com.cortex.sign.integration.assinatura.ProvedorAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.DadosAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.ResultadoAssinatura;
import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.assinatura.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockProvedorAssinatura implements ProvedorAssinatura {

    @Override
    public ResultadoAssinatura assinar(DadosAssinatura dadosAssinatura) {
        String protocolo = "MOCK-" + UUID.randomUUID();
        String metadados = "Assinatura mock para desenvolvimento; documentoId="
                + dadosAssinatura.documentoId()
                + "; signatarioId="
                + dadosAssinatura.signatarioId();

        return new ResultadoAssinatura(
                getProviderType(),
                protocolo,
                LocalDateTime.now(),
                metadados
        );
    }

    @Override
    public boolean validar(String protocolo) {
        return protocolo != null && protocolo.startsWith("MOCK-");
    }

    @Override
    public TipoProvedorAssinatura getProviderType() {
        return TipoProvedorAssinatura.MOCK;
    }
}
