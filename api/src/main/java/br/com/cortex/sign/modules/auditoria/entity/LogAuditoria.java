package br.com.cortex.sign.modules.auditoria.entity;

import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    private Organizacao organizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String acao;

    @Column(name = "entidade_tipo", nullable = false, length = 80)
    private String entidadeTipo;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
