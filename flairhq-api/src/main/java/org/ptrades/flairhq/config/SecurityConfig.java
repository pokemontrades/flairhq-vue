package org.ptrades.flairhq.config;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler loginSuccessHandler;
    private final AccessDeniedLoggingHandler accessDeniedLoggingHandler;
    private final ServiceKeyAuthFilter serviceKeyAuthFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public SecurityConfig(OAuth2LoginSuccessHandler loginSuccessHandler,
                          AccessDeniedLoggingHandler accessDeniedLoggingHandler,
                          ServiceKeyAuthFilter serviceKeyAuthFilter) {
        this.loginSuccessHandler  = loginSuccessHandler;
        this.accessDeniedLoggingHandler = accessDeniedLoggingHandler;
        this.serviceKeyAuthFilter = serviceKeyAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        http
            .addFilterBefore(serviceKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/actuator/health",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                // Check mod permissions and upsert user record after every Reddit login
                .successHandler(loginSuccessHandler)
                .failureUrl(frontendUrl + "/login?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(2) // only allow 2 concurrent sessions per user
                .sessionRegistry(sessionRegistry) // registry is used for mod-initiated invalidation
                .expiredSessionStrategy(event -> event.getResponse().sendError(401))
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedLoggingHandler)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF disabled — API is consumed by a separate SPA origin
            .csrf(csrf -> csrf.disable()); // TODO: how can I enable CSRF protection

        return http.build();
    }

    @Bean
    public SpringSessionBackedSessionRegistry<? extends Session> sessionRegistry(
            FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        return new SpringSessionBackedSessionRegistry<>(sessionRepository);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Required so the browser sends the session cookie cross-origin
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
