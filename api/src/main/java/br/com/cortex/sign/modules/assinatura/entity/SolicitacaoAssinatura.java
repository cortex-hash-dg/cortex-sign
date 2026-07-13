package br.com.cortex.sign.modules.assinatura.entity;

import br.com.cortex.sign.modules.assinatura.enums.StatusSolicitacaoAssinatura;
import br.com.cortex.sign.modules.documento.entity.Documento;
import br.com.cortex.sign.modules.signatario.entity.Signatario;
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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Entity
@Table(name = "solicitacoes_assinatura")
public class SolicitacaoAssinatura {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signatario_id", nullable = false)
    private Signatario signatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusSolicitacaoAssinatura status;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @Column(name = "codigo_hash", nullable = false, length = 128)
    private String codigoHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
