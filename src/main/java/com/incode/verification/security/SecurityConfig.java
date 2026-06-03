package com.incode.verification.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] OPEN_PATHS = {
        "/free-third-party/**",
        "/premium-third-party/**",
        "/actuator/health/**",
        "/actuator/info",
        "/actuator/prometheus",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    private void sharedRules(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, OPEN_PATHS)
                        .permitAll()
                        .requestMatchers(OPEN_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated());
    }

    @Bean
    @Profile("!aws")
    public SecurityFilterChain basicSecurityFilterChain(HttpSecurity http) throws Exception {
        sharedRules(http);
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Profile("!aws")
    public UserDetailsService inMemoryUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("verifier")
                        .password("{noop}verifier-pass")
                        .roles("VERIFIER")
                        .build(),
                User.withUsername("auditor")
                        .password("{noop}auditor-pass")
                        .roles("AUDITOR")
                        .build(),
                User.withUsername("admin")
                        .password("{noop}admin-pass")
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    @Profile("aws")
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        sharedRules(http);
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new java.util.ArrayList<>(scopes.convert(jwt));
            var roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            }
            return authorities;
        });
        return converter;
    }
}
