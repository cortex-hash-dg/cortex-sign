package br.com.cortex.sign.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.cortex.sign.dto.CriarOrganizacaoRequest;
import br.com.cortex.sign.dto.OrganizacaoResponse;
import br.com.cortex.sign.service.OrganizacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizacoes")
@RequiredArgsConstructor
public class OrganizacaoController {

    private final OrganizacaoService organizacaoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(@Valid @RequestBody CriarOrganizacaoRequest request) {
        return organizacaoService.criar(request);
    }

    @GetMapping
    public List<OrganizacaoResponse> listar() {
        return organizacaoService.listar();
    }
}