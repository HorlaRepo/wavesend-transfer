package com.shizzy.moneytransfer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    @Value("${springdoc.server-url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        Server productionServer = new Server();
        productionServer.setUrl(serverUrl.isEmpty() ? 
                "https://wavesend-api-gvfzc6hvfqhuemad.eastus2-01.azurewebsites.net" : 
                serverUrl);
        productionServer.setDescription("Production Server");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local Development");

        return new OpenAPI()
                .servers(List.of(productionServer, localServer))
                .info(new Info()
                        .title("WaveSend API")
                        .description("API for the WaveSend Money Transfer Service")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Francis Oladosu")
                                .email("support@wavesend.com"))
                        .license(new License()
                                .name("Private API")
                                .url("https://wavesend.com/terms")));
    }
}