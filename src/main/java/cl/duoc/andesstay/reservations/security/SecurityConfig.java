package cl.duoc.andesstay.reservations.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers("/actuator/**").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reservations"
                        ).hasAnyRole(
                                "CLIENTE",
                                "OPERADOR",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/confirm"
                        ).hasAnyRole(
                                "OPERADOR",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/reservations/*/cancel"
                        ).hasAnyRole(
                                "OPERADOR",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/reservations/*/status"
                        ).hasAnyRole(
                                "OPERADOR",
                                "ADMIN"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reservations/me"
                        ).hasAnyRole("CLIENTE", "ADMIN")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/reservations",
                                "/api/reservations/*"
                        ).hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                jwtAuthenticationConverter()
                        ))
                );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }
}
