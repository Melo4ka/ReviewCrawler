package ru.digitalhoreca.reviewcrawler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@EnableConfigurationProperties
class ReviewCrawlerApplication

fun main(args: Array<String>) {
    runApplication<ReviewCrawlerApplication>(*args)
}
