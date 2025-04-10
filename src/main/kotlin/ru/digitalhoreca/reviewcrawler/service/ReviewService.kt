package ru.digitalhoreca.reviewcrawler.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhoreca.reviewcrawler.dto.review.ReviewCreateDto
import ru.digitalhoreca.reviewcrawler.dto.review.ReviewDto
import ru.digitalhoreca.reviewcrawler.entity.Review
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import ru.digitalhoreca.reviewcrawler.repository.CompanyRepository
import ru.digitalhoreca.reviewcrawler.repository.ReviewRepository

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val companyRepository: CompanyRepository
) {

    private val logger = LoggerFactory.getLogger(ReviewService::class.java)

    companion object {
        private const val MAX_TEXT_LENGTH = 65000
    }

    fun getReviewsByCompanyIds(companyIds: List<Long>, source: ReviewSource?, pageable: Pageable) =
        if (source != null) {
            reviewRepository.findAllByCompanyIdInAndSource(companyIds, source, pageable)
        } else {
            reviewRepository.findAllByCompanyIdIn(companyIds, pageable)
        }.map { it.toDto() }

    fun findLastReviewByCompanyIdAndSource(companyId: Long, source: ReviewSource): Review? {
        return reviewRepository.findTopByCompanyIdAndSourceOrderByDateDesc(companyId, source)
    }

    fun existsByExternalIdAndSource(externalId: String, source: ReviewSource): Boolean {
        return reviewRepository.findByExternalIdAndSource(externalId, source) != null
    }

    @Transactional
    fun saveReview(reviewDto: ReviewCreateDto): Review {
        val company = companyRepository.findById(reviewDto.companyId)
            .orElseThrow { IllegalArgumentException("Компания не найдена с ID: ${reviewDto.companyId}") }

        var reviewText = reviewDto.text
        if (reviewText.length > MAX_TEXT_LENGTH) {
            logger.warn("Текст отзыва с ID ${reviewDto.externalId} слишком длинный (${reviewText.length} символов). Обрезаем до $MAX_TEXT_LENGTH символов.")
            reviewText = reviewText.substring(0, MAX_TEXT_LENGTH)
        }

        val review = Review(
            company = company,
            source = reviewDto.source,
            externalId = reviewDto.externalId,
            text = reviewText,
            authorName = reviewDto.authorName,
            rating = reviewDto.rating,
            date = reviewDto.date,
            imageUrls = reviewDto.imageUrls.toMutableList()
        )

        return reviewRepository.save(review)
    }

    private fun Review.toDto() = ReviewDto(
        id = this.id,
        companyId = this.company.id!!,
        source = this.source,
        externalId = this.externalId,
        text = this.text,
        authorName = this.authorName,
        rating = this.rating,
        date = this.date,
        imageUrls = this.imageUrls
    )
}
