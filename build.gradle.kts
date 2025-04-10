plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "ru.digitalhoreca"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot основные зависимости
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI и Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

    // Kotlin поддержка
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // ShedLock для планировщика
    implementation("net.javacrumbs.shedlock:shedlock-spring:5.12.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.12.0")

    // Selenium для краулинга - обновленные версии
    implementation("org.seleniumhq.selenium:selenium-java:4.31.0")
    implementation("org.seleniumhq.selenium:selenium-chrome-driver:4.31.0")
    implementation("org.seleniumhq.selenium:selenium-devtools-v135:4.31.0")
    implementation("io.github.bonigarcia:webdrivermanager:5.7.0")

    // База данных и пул соединений
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    implementation("com.zaxxer:HikariCP")

    // Логирование
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Тестирование
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2") // БД для тестов
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("review-crawler.jar")
}
