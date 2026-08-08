package br.com.cortex.sign.modules.assinatura.entity;

import br.com.cortex.sign.modules.assinatura.enums.CanalCodigoAssinatura;
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
@Table(name = "acessos_signatario_externo")
public class AcessoSignatarioExterno {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitacao_assinatura_id", nullable = false)
    private SolicitacaoAssinatura solicitacaoAssinatura;

    @Column(name = "cpf_hash", nullable = false, length = 128)
    private String cpfHash;

    @Column(name = "codigo_hash", nullable = false, length = 128)
    private String codigoHash;

    @Column(name = "token_acesso_hash", length = 128)
    private String tokenAcessoHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalCodigoAssinatura canal;

    @Column(name = "destino_mascarado", length = 150)
    private String destinoMascarado;

    @Column(name = "codigo_expira_em", nullable = false)
    private LocalDateTime codigoExpiraEm;

    @Column(name = "token_expira_em")
    private LocalDateTime tokenExpiraEm;

    @Column(name = "validado_em")
    private LocalDateTime validadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
