package br.com.cortex.sign.modules.assinatura.entity;

import br.com.cortex.sign.integration.assinatura.enums.TipoProvedorAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.StatusAssinatura;
import br.com.cortex.sign.modules.assinatura.enums.TipoAssinatura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "assinaturas")
public class Assinatura {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_assinatura_id", nullable = false)
    private SolicitacaoAssinatura solicitacaoAssinatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoAssinatura tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusAssinatura status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private TipoProvedorAssinatura provedor;

    @Column(length = 120)
    private String protocolo;

    @Column(name = "assinado_em")
    private LocalDateTime assinadoEm;

    @Column(name = "rejeitado_em")
    private LocalDateTime rejeitadoEm;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "ip_assinatura", length = 80)
    private String ipAssinatura;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String metadados;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
