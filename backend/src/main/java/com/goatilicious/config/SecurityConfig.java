package com.goatilicious.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Separação Cliente / Staff com sessão no servidor e cookie HttpOnly.
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
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${goatilicious.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpStatusEntryPoint semDesafio = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        RequestMatcher csrfProtection = request -> {
            if (!Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod())) {
                return false;
            }
            if (request.getHeader("Authorization") != null
                    || antMatcher(HttpMethod.POST, "/api/clientes").matches(request)) {
                return false;
            }
            if (request.getCookies() == null) {
                return false;
            }
            return Arrays.stream(request.getCookies())
                    .anyMatch(cookie -> "JSESSIONID".equals(cookie.getName())
                            || "XSRF-TOKEN".equals(cookie.getName()));
        };

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfHandler)
                        .requireCsrfProtectionMatcher(csrfProtection))
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(session -> session.migrateSession()))
                // Mantido para clientes legados; o frontend já usa sessão por cookie.
                .httpBasic(basic -> basic.authenticationEntryPoint(semDesafio))
                .exceptionHandling(e -> e.authenticationEntryPoint(semDesafio))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/auth/csrf")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                        // público: cadastro e catálogo
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/clientes")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.GET, "/api/produtos")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/logout")).authenticated()
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
