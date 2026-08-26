package br.com.comandavision.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    private static final String ESQUEMA_SEGURANCA = "bearerAuth";

    @Bean
    public OpenAPI configurarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ComandaVision API")
                        .description("API para gerenciamento de comandas e análise de vendas")
                        .version("1.0"))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(ESQUEMA_SEGURANCA))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        ESQUEMA_SEGURANCA,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
