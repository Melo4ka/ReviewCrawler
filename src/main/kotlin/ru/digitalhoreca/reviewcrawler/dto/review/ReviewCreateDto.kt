package ru.digitalhoreca.reviewcrawler.dto.review

import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import java.util.*

data class ReviewCreateDto(
    val companyId: Long,
    val source: ReviewSource,
    val externalId: String,
    val text: String,
    val authorName: String,
    val rating: Float,
    val date: Date,
    val imageUrls: List<String> = emptyList()
)
