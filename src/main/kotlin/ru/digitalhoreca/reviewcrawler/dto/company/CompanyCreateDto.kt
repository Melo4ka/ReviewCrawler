package ru.digitalhoreca.reviewcrawler.dto.company

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DTO для создания новой компании")
data class CompanyCreateDto(
    @Schema(description = "Название компании", example = "Ресторан Васильки")
    val name: String,

    @Schema(description = "Адрес компании", example = "г. Москва, ул. Пушкина, д. 10")
    val address: String,

    @Schema(description = "Идентификатор компании в Yandex Maps", example = "12345")
    val yandexId: Long?,

    @Schema(description = "Идентификатор компании в 2GIS", example = "67890")
    val twoGisId: Long?
)