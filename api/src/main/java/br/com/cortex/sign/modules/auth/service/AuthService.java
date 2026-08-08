package br.com.cortex.sign.modules.auth.service;

import br.com.cortex.sign.common.exception.ConflitoException;
import br.com.cortex.sign.common.exception.CredenciaisInvalidasException;
import br.com.cortex.sign.modules.auth.dto.request.AlterarSenhaRequest;
import br.com.cortex.sign.modules.auth.dto.request.AtualizarCadastroUsuarioRequest;
import br.com.cortex.sign.modules.auth.dto.request.AtualizarAssinaturaUsuarioRequest;
import br.com.cortex.sign.modules.auth.dto.request.ConfirmarEmailRequest;
import br.com.cortex.sign.modules.auth.dto.request.LoginRequest;
import br.com.cortex.sign.modules.auth.dto.request.LogoutRequest;
import br.com.cortex.sign.modules.auth.dto.request.RenovarTokenRequest;
import br.com.cortex.sign.modules.auth.dto.response.AssinaturaUsuarioResponse;
import br.com.cortex.sign.modules.auth.dto.response.CadastroUsuarioResponse;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmacaoEmailSolicitadaResponse;
import br.com.cortex.sign.modules.auth.dto.response.ConfirmarEmailResponse;
import br.com.cortex.sign.modules.auth.dto.response.LoginResponse;
import br.com.cortex.sign.modules.auth.dto.response.UsuarioAutenticadoResponse;
import br.com.cortex.sign.modules.auth.jwt.JwtService;
import br.com.cortex.sign.modules.auth.jwt.JwtTokenGerado;
import br.com.cortex.sign.modules.auth.jwt.UsuarioAutenticado;
import br.com.cortex.sign.modules.organizacao.dto.response.OrganizacaoMembroResponse;
import br.com.cortex.sign.modules.organizacao.entity.Organizacao;
import br.com.cortex.sign.modules.organizacao.service.OrganizacaoMembroService;
import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenAtualizacaoService tokenAtualizacaoService;
    private final OrganizacaoMembroService organizacaoMembroService;
    private final EmailConfirmacaoService emailConfirmacaoService;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(normalizarEmail(request.email()))
                .orElseThrow(() -> new CredenciaisInvalidasException("Credenciais inválidas"));

        validarUsuarioAtivo(usuario);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }

        TokenAtualizacaoGerado tokenAtualizacao = tokenAtualizacaoService.criar(usuario, servletRequest);
        return criarRespostaAutenticacao(usuario, tokenAtualizacao);
    }

    @Transactional
    public LoginResponse renovarToken(RenovarTokenRequest request, HttpServletRequest servletRequest) {
        TokenAtualizacaoRotacionado tokenRotacionado = tokenAtualizacaoService.rotacionar(
                request.tokenAtualizacao(),
                servletRequest
        );

        return criarRespostaAutenticacao(tokenRotacionado.usuario(), tokenRotacionado.tokenAtualizacao());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        tokenAtualizacaoService.revogar(request.tokenAtualizacao());
    }

    @Transactional(readOnly = true)
    public UsuarioAutenticadoResponse me(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        Organizacao organizacao = usuario.getOrganizacao();

        return new UsuarioAutenticadoResponse(
                usuario.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getNomeSocial(),
                usuario.getDataNascimento(),
                usuario.getEmailVerificado(),
                usuario.getTelefoneVerificado()
        );
    }

    @Transactional(readOnly = true)
    public CadastroUsuarioResponse buscarCadastro(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        return montarCadastroResponse(usuario);
    }

    @Transactional
    public CadastroUsuarioResponse atualizarCadastro(
            UsuarioAutenticado usuarioAutenticado,
            AtualizarCadastroUsuarioRequest request
    ) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);

        String email = normalizarEmail(request.email());
        String cpf = normalizarCpf(request.cpf());
        String telefone = normalizarTelefone(request.telefone());

        validarCadastroUnico(usuario, email, cpf, telefone);

        if (!usuario.getEmail().equalsIgnoreCase(email)) {
            usuario.setEmailVerificado(false);
        }

        if (!valoresIguais(usuario.getTelefone(), telefone)) {
            usuario.setTelefoneVerificado(false);
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setCpf(cpf);
        usuario.setTelefone(telefone);
        usuario.setNomeSocial(normalizarTextoOpcional(request.nomeSocial()));
        usuario.setDataNascimento(request.dataNascimento());

        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);
        return montarCadastroResponse(usuarioSalvo);
    }

    @Transactional
    public ConfirmacaoEmailSolicitadaResponse solicitarConfirmacaoEmail(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        return emailConfirmacaoService.solicitar(usuario);
    }

    @Transactional
    public ConfirmarEmailResponse confirmarEmail(ConfirmarEmailRequest request) {
        return emailConfirmacaoService.confirmar(request.token());
    }

    @Transactional(readOnly = true)
    public List<OrganizacaoMembroResponse> minhasOrganizacoes(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        return organizacaoMembroService.listarPorUsuario(usuarioAutenticado, usuario.getId());
    }


    @Transactional(readOnly = true)
    public AssinaturaUsuarioResponse buscarAssinatura(UsuarioAutenticado usuarioAutenticado) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        return montarAssinaturaResponse(usuario);
    }

    @Transactional
    public AssinaturaUsuarioResponse atualizarAssinatura(UsuarioAutenticado usuarioAutenticado, AtualizarAssinaturaUsuarioRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);
        usuario.setNomeAssinatura(normalizarTextoOpcional(request.nomeAssinatura()));
        usuario.setAssinaturaManuscritaBase64(normalizarAssinaturaManuscrita(request.assinaturaManuscritaBase64()));
        Usuario usuarioSalvo = usuarioRepository.saveAndFlush(usuario);
        return montarAssinaturaResponse(usuarioSalvo);
    }

    @Transactional
    public void alterarSenha(UsuarioAutenticado usuarioAutenticado, AlterarSenhaRequest request) {
        Usuario usuario = buscarUsuarioAutenticado(usuarioAutenticado);

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException("Senha atual inválida");
        }

        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);
        tokenAtualizacaoService.revogarTodosDoUsuario(usuario);
    }

    private LoginResponse criarRespostaAutenticacao(Usuario usuario, TokenAtualizacaoGerado tokenAtualizacao) {
        JwtTokenGerado tokenAcesso = jwtService.gerarToken(usuario);
        Organizacao organizacao = usuario.getOrganizacao();

        return new LoginResponse(
                tokenAcesso.token(),
                tokenAtualizacao.token(),
                "Bearer",
                tokenAcesso.expiraEm(),
                tokenAtualizacao.expiraEm(),
                usuario.getId(),
                organizacao != null ? organizacao.getId() : null,
                organizacao != null ? organizacao.getNome() : null,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }


    private AssinaturaUsuarioResponse montarAssinaturaResponse(Usuario usuario) {
        String assinatura = usuario.getAssinaturaManuscritaBase64();
        return new AssinaturaUsuarioResponse(
                usuario.getNomeAssinatura() != null ? usuario.getNomeAssinatura() : usuario.getNome(),
                assinatura,
                assinatura != null && !assinatura.isBlank()
        );
    }

    private CadastroUsuarioResponse montarCadastroResponse(Usuario usuario) {
        return new CadastroUsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getTelefone(),
                usuario.getNomeSocial(),
                usuario.getDataNascimento(),
                usuario.getEmailVerificado(),
                usuario.getTelefoneVerificado()
        );
    }

    private void validarCadastroUnico(Usuario usuario, String email, String cpf, String telefone) {
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, usuario.getId())) {
            throw new ConflitoException("Já existe um usuário com este e-mail");
        }

        if (cpf != null && usuarioRepository.existsByCpfAndIdNot(cpf, usuario.getId())) {
            throw new ConflitoException("Já existe um usuário com este CPF");
        }

        if (telefone != null && usuarioRepository.existsByTelefoneAndIdNot(telefone, usuario.getId())) {
            throw new ConflitoException("Já existe um usuário com este telefone");
        }
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    private String normalizarAssinaturaManuscrita(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        String assinatura = valor.trim();
        if (assinatura.startsWith("data:image/")) {
            return assinatura;
        }

        return "data:image/png;base64," + assinatura;
    }

    private String normalizarCpf(String valor) {
        String cpf = manterApenasDigitos(valor);
        if (cpf == null) {
            return null;
        }

        if (cpf.length() != 11) {
            throw new ConflitoException("CPF deve conter 11 dígitos");
        }

        return cpf;
    }

    private String normalizarTelefone(String valor) {
        String telefone = manterApenasDigitos(valor);
        if (telefone == null) {
            return null;
        }

        if (telefone.length() < 10 || telefone.length() > 15) {
            throw new ConflitoException("Telefone deve conter entre 10 e 15 dígitos");
        }

        return telefone;
    }

    private String manterApenasDigitos(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.replaceAll("\\D", "");
    }

    private boolean valoresIguais(String primeiroValor, String segundoValor) {
        if (primeiroValor == null) {
            return segundoValor == null;
        }

        return primeiroValor.equals(segundoValor);
    }

    private Usuario buscarUsuarioAutenticado(UsuarioAutenticado usuarioAutenticado) {
        if (usuarioAutenticado == null) {
            throw new CredenciaisInvalidasException("Autenticação obrigatória");
        }

        Usuario usuario = usuarioRepository.findById(usuarioAutenticado.id())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário não encontrado"));

        validarUsuarioAtivo(usuario);

        return usuario;
    }

    private void validarUsuarioAtivo(Usuario usuario) {
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CredenciaisInvalidasException("Usuário inativo");
        }
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
