package br.com.cortex.sign.modules.organizacao.entity;

import br.com.cortex.sign.modules.organizacao.enums.PapelOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusMembroOrganizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
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
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "organizacoes_membros",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_organizacoes_membros_usuario_organizacao",
                columnNames = {"usuario_id", "organizacao_id"}
        )
)
public class OrganizacaoMembro {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizacao_id", nullable = false)
    private Organizacao organizacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PapelOrganizacao papel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusMembroOrganizacao status = StatusMembroOrganizacao.ATIVO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convidado_por_usuario_id")
    private Usuario convidadoPorUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "papel_alterado_por_usuario_id")
    private Usuario papelAlteradoPorUsuario;

    @Column(name = "grupos", columnDefinition = "TEXT")
    private String grupos;

    @Column(name = "permissoes_adicionais", columnDefinition = "TEXT")
    private String permissoesAdicionais;

    @Column(name = "permissoes_removidas", columnDefinition = "TEXT")
    private String permissoesRemovidas;

    @Column(name = "motivo_suspensao", length = 500)
    private String motivoSuspensao;

    @Column(name = "convidado_em")
    private LocalDateTime convidadoEm;

    @Column(name = "aceito_em")
    private LocalDateTime aceitoEm;

    @Column(name = "papel_alterado_em")
    private LocalDateTime papelAlteradoEm;

    @Column(name = "suspenso_em")
    private LocalDateTime suspensoEm;

    @Column(name = "removido_em")
    private LocalDateTime removidoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
