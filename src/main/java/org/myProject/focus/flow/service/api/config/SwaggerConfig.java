package org.myProject.focus.flow.service.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:6231}") // Default value is 6231 if the property is not set
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(
                        List.of(new Server().url("http://localhost:" + serverPort))
                )
                .info(new Info()
                        .title("Focus Flow Task API")
                        .description("A microservice for managing tasks and projects. " +
                                "It allows creating, editing, and deleting tasks, " +
                                "as well as managing their statuses and priorities.")
                        .version("1.0.0")
                        .license(new io.swagger.v3.oas.models.info.License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0"))
                        .contact(new io.swagger.v3.oas.models.info.Contact()
                                .name("Focus Flow Support"))
                );
    }
}