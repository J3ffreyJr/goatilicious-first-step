package com.goatilicious.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Separação Cliente / Staff. HTTP Basic sem sessão (só seguro com HTTPS em produção).
 * Sem credenciais: 401 (sem cabeçalho WWW-Authenticate, para o browser não abrir o seu pop-up).
 * Papel errado: 403. A regra "o cliente só vê o que é dele" está nos serviços (404).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpStatusEntryPoint semDesafio = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(semDesafio))
                .exceptionHandling(e -> e.authenticationEntryPoint(semDesafio))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/error")).permitAll()
                        // público: cadastro e catálogo
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/clientes")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/produtos")).permitAll()
                        // qualquer conta autenticada
                        .requestMatchers(antMatcher("/api/me")).authenticated()
                        // cliente (só o que é seu, aplicado nos serviços) e staff
                        .requestMatchers(
                                antMatcher(HttpMethod.POST, "/api/pedidos"),
                                antMatcher(HttpMethod.GET, "/api/pedidos"),
                                antMatcher(HttpMethod.GET, "/api/pedidos/*"),
                                antMatcher(HttpMethod.GET, "/api/pedidos/*/factura"),
                                antMatcher(HttpMethod.GET, "/api/facturas/*"),
                                antMatcher(HttpMethod.GET, "/api/facturas/*/pdf")).hasAnyRole("CLIENTE", "STAFF")
                        // todo o resto: só staff
                        .anyRequest().hasRole("STAFF"));
        return http.build();
    }
}
