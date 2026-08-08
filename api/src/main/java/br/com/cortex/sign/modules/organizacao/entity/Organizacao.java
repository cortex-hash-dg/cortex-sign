package br.com.cortex.sign.modules.organizacao.entity;

import br.com.cortex.sign.modules.organizacao.enums.StatusOrganizacao;
import br.com.cortex.sign.modules.organizacao.enums.StatusVerificacaoOrganizacao;
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
@Table(name = "organizacoes")
public class Organizacao {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @Column(name = "razao_social", length = 180)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 150)
    private String nomeFantasia;

    @Column(length = 14)
    private String cnpj;

    @Column(name = "email_corporativo", length = 150)
    private String emailCorporativo;

    @Column(length = 30)
    private String telefone;

    @Column(length = 20)
    private String cep;

    @Column(length = 180)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 120)
    private String complemento;

    @Column(length = 120)
    private String bairro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(length = 80)
    private String pais;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusOrganizacao status = StatusOrganizacao.ATIVA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proprietario_usuario_id")
    private Usuario proprietarioUsuario;

    @Column(length = 80)
    private String plano;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_verificacao", nullable = false, length = 40)
    private StatusVerificacaoOrganizacao statusVerificacao = StatusVerificacaoOrganizacao.PENDENTE;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
