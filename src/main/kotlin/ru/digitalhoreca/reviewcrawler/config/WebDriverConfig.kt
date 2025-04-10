package ru.digitalhoreca.reviewcrawler.config

import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebDriverConfig {

    @Value("\${crawler.userAgent}")
    private lateinit var userAgent: String

    @Bean
    fun chromeOptions() = ChromeOptions().apply {
        setBinary("/usr/bin/chromium")
        addArguments("--headless=new")
        addArguments("--no-sandbox")
        addArguments("--disable-dev-shm-usage")
        addArguments("--disable-gpu")
        addArguments("--remote-allow-origins=*")
        addArguments("--window-size=1920,1080")
        addArguments("--disable-extensions")
        addArguments("--disable-browser-side-navigation")
        addArguments("--ignore-certificate-errors")
        addArguments("--user-agent=$userAgent")
    }
}
