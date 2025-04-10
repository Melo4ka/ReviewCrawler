package ru.digitalhoreca.reviewcrawler.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Review Crawler API")
                .description("API для сервиса сбора и обработки отзывов")
        )
        .addServersItem(
            Server()
                .url("/")
                .description("Server URL")
        )
} 