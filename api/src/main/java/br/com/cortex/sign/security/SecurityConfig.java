package br.com.cortex.sign.security;

import br.com.cortex.sign.modules.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> escreverErro(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Autenticação obrigatória"
                        ))
                        .accessDeniedHandler((request, response, accessDeniedException) -> escreverErro(
                                response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "Acesso negado"
                        ))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/email/confirmacao/validar").permitAll()
                        .requestMatchers("/api/publico/assinaturas/**").permitAll()
                        .requestMatchers("/api/webhooks/whatsapp").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/certificados/verificar/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/certificados/verificar/documento").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/organizacoes/*/membros/**").authenticated()
                        .requestMatchers("/api/organizacoes/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO"
                        )
                        .requestMatchers("/api/usuarios/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO"
                        )
                        .requestMatchers(HttpMethod.POST, "/api/documentos/*/signatarios").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/documentos/*/signatarios").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR",
                                "AUDITOR"
                        )
                        .requestMatchers(HttpMethod.POST, "/api/documentos/*/solicitacoes-assinatura").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/documentos/*/solicitacoes-assinatura").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR",
                                "AUDITOR"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/documentos/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR",
                                "AUDITOR"
                        )
                        .requestMatchers(HttpMethod.POST, "/api/documentos/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers(HttpMethod.PUT, "/api/documentos/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR"
                        )
                        .requestMatchers(HttpMethod.GET, "/api/signatarios/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR",
                                "AUDITOR"
                        )
                        .requestMatchers("/api/signatarios/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers("/api/solicitacoes-assinatura/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "GESTOR",
                                "OPERADOR"
                        )
                        .requestMatchers("/api/auditorias/**").hasAnyRole(
                                "SUPER_ADMINISTRADOR",
                                "ADMINISTRADOR_ORGANIZACAO",
                                "AUDITOR"
                        )
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Usuário não encontrado");
        };
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private void escreverErro(HttpServletResponse response, int status, String mensagem) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensagem\":\"" + mensagem + "\"}");
    }
}
