package br.com.cortex.sign.modules.certificado.entity;

import br.com.cortex.sign.modules.assinatura.entity.Assinatura;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Entity
@Table(name = "certificados")
public class Certificado {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private Assinatura assinatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signatario_final_id", nullable = false)
    private Signatario signatarioFinal;

    @Column(name = "hash_documento", nullable = false, length = 64)
    private String hashDocumento;

    @Column(name = "assinatura_digital", nullable = false, columnDefinition = "TEXT")
    private String assinaturaDigital;

    @Column(name = "dados_autenticacao", columnDefinition = "TEXT")
    private String dadosAutenticacao;

    @Column(name = "url_documento", length = 500)
    private String urlDocumento;

    @Column(name = "url_verificacao", nullable = false, length = 500)
    private String urlVerificacao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
