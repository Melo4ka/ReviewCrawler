package ru.digitalhoreca.reviewcrawler.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourceConfig {

    @Value("\${spring.datasource.url}")
    private lateinit var jdbcUrl: String

    @Value("\${spring.datasource.username}")
    private lateinit var username: String

    @Value("\${spring.datasource.password}")
    private lateinit var password: String

    @Value("\${spring.datasource.driver-class-name}")
    private lateinit var driverClassName: String

    @Bean
    @Primary
    fun dataSource(): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = this@DataSourceConfig.jdbcUrl
            username = this@DataSourceConfig.username
            password = this@DataSourceConfig.password
            driverClassName = this@DataSourceConfig.driverClassName

            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000
            connectionTimeout = 20000
            maxLifetime = 1800000

            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000

            poolName = "ReviewCrawlerHikariCP"
        }

        return HikariDataSource(config)
    }
}
