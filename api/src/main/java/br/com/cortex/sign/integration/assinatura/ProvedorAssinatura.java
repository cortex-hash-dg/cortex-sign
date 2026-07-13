package br.com.cortex.sign.integration.assinatura;

import br.com.cortex.sign.integration.assinatura.dto.DadosAssinatura;
import br.com.cortex.sign.integration.assinatura.dto.ResultadoAssinatura;
import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;

public interface ProvedorAssinatura {

    ResultadoAssinatura assinar(DadosAssinatura dadosAssinatura);

    boolean validar(String protocolo);

    TipoProvedorAssinatura getProviderType();
}
