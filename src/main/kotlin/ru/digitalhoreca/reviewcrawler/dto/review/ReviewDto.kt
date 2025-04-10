package ru.digitalhoreca.reviewcrawler.dto.review

import io.swagger.v3.oas.annotations.media.Schema
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import java.util.*

@Schema(description = "DTO отзыва")
data class ReviewDto(
    @Schema(description = "Уникальный идентификатор отзыва", example = "1")
    val id: Long?,

    @Schema(description = "Идентификатор компании", example = "42")
    val companyId: Long,

    @Schema(description = "Источник отзыва (YANDEX_MAPS, TWO_GIS)", example = "YANDEX_MAPS")
    val source: ReviewSource,

    @Schema(description = "Внешний идентификатор отзыва в системе-источнике", example = "yand123456")
    val externalId: String,

    @Schema(description = "Текст отзыва", example = "Отличное заведение, всем рекомендую!")
    val text: String,

    @Schema(description = "Имя автора отзыва", example = "Иван Петров")
    val authorName: String,

    @Schema(description = "Рейтинг (оценка)", example = "4.5")
    val rating: Float,

    @Schema(description = "Дата публикации отзыва")
    val date: Date,

    @Schema(description = "Список URL изображений, прикрепленных к отзыву")
    val imageUrls: List<String>
)
