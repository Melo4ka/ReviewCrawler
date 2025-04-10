package ru.digitalhoreca.reviewcrawler.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate() = RestTemplate().apply {
        messageConverters.add(MappingJackson2HttpMessageConverter())
    }
}
