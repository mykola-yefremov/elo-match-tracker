package com.emt.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Elo Match Tracker",
            version = "0.0.1",
            description =
                "Server-rendered Elo ranking application. REST endpoints can be added later on top of the existing service layer."),
    servers = @Server(url = "http://localhost:8080", description = "Local server"))
public class OpenApiConfiguration {}
