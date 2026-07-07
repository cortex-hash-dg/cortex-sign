package br.com.cortex.sign.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cortex.sign.entity.Organizacao;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, UUID> {
}