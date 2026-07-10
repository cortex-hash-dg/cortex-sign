package br.com.cortex.sign.modules.auth.bootstrap;

import br.com.cortex.sign.modules.usuario.entity.Usuario;
import br.com.cortex.sign.modules.usuario.enums.PerfilUsuario;
import br.com.cortex.sign.modules.usuario.repository.UsuarioRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminSeedRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.super-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.super-admin.name:Super Administrador}")
    private String name;

    @Value("${app.seed.super-admin.email:}")
    private String email;

    @Value("${app.seed.super-admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        validarConfiguracao();

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            log.info("Seed de Super Admin ignorado: usuário {} já existe", emailNormalizado);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(name == null || name.isBlank() ? "Super Administrador" : name.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(password));
        usuario.setPerfil(PerfilUsuario.SUPER_ADMINISTRADOR);
        usuario.setAtivo(true);
        usuario.setOrganizacao(null);

        usuarioRepository.save(usuario);

        log.info("Usuário Super Admin inicial criado: {}", emailNormalizado);
    }

    private void validarConfiguracao() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("SEED_SUPER_ADMIN_EMAIL é obrigatório quando o seed está ativo");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalStateException("SEED_SUPER_ADMIN_PASSWORD deve ter pelo menos 8 caracteres");
        }
    }
}
