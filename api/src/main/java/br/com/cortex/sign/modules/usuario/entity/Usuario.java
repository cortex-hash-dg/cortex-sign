package br.com.cortex.sign.modules.usuario.entity;

import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
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
import java.time.LocalDate;
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
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_id")
    private Organizacao organizacao;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 11)
    private String cpf;

    @Column(length = 30)
    private String telefone;

    @Column(name = "nome_social", length = 150)
    private String nomeSocial;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    @Column(name = "telefone_verificado", nullable = false)
    private Boolean telefoneVerificado = false;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "assinatura_manuscrita_base64", columnDefinition = "TEXT")
    private String assinaturaManuscritaBase64;

    @Column(name = "nome_assinatura", length = 150)
    private String nomeAssinatura;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
