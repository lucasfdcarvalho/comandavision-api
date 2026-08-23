package br.com.comandavision.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtAuthenticationConverter,
                        AutenticacaoNaoRealizadaHandler autenticacaoHandler,
                        AcessoNegadoHandler acessoNegadoHandler)
                        throws Exception {

                http.csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/api/dashboard/**").hasRole("DONO")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .authenticationEntryPoint(autenticacaoHandler)
                                                .accessDeniedHandler(acessoNegadoHandler)
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter)));

                return http.build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter conversorDePapeis = new JwtGrantedAuthoritiesConverter();

                conversorDePapeis.setAuthoritiesClaimName("user_role");
                conversorDePapeis.setAuthorityPrefix("ROLE_");

                JwtAuthenticationConverter conversorJwt = new JwtAuthenticationConverter();

                conversorJwt.setJwtGrantedAuthoritiesConverter(conversorDePapeis);

                return conversorJwt;
        }
}
