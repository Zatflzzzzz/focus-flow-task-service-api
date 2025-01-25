package org.myProject.focus.flow.service.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(
                        List.of(
                                new Server().url("http://localhost:1111")
                        )
                )
                .info(
                        new Info()
                                .title("My API")
                                .description("API documentation")
                                .version("1.0.0")
                );
    }
}
