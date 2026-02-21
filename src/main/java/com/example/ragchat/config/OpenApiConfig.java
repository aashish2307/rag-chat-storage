package com.example.ragchat.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String API_KEY_SCHEME = "apiKey";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("API key for authentication (e.g. dev-api-key). Also add header X-User-Id for user scope.")))
                .security(List.of(new SecurityRequirement().addList(API_KEY_SCHEME)));
    }

    /**
     * Document "sort" as a string (e.g. "createdAt,desc") so Swagger UI does not send invalid array like ["string"].
     */
    @Bean
    public OperationCustomizer sortParameterAsString() {
        return (operation, handlerMethod) -> {
            if (operation.getParameters() == null) return operation;
            for (Parameter p : operation.getParameters()) {
                if (p != null && "sort".equals(p.getName())) {
                    p.setSchema(new Schema<>().type("string").example("createdAt,desc")
                            .description("Sort: property,direction. E.g. createdAt,desc or title,asc. Omit for default order."));
                    p.setRequired(false);
                    break;
                }
            }
            return operation;
        };
    }
}
