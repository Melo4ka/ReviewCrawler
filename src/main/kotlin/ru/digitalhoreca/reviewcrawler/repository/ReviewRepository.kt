package ru.digitalhoreca.reviewcrawler.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.digitalhoreca.reviewcrawler.entity.Review
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource

interface ReviewRepository : JpaRepository<Review, Long> {

    fun findAllByCompanyIdIn(companyIds: List<Long>, pageable: Pageable): Page<Review>

    fun findAllByCompanyIdInAndSource(companyIds: List<Long>, source: ReviewSource, pageable: Pageable): Page<Review>

    fun findTopByCompanyIdAndSourceOrderByDateDesc(companyId: Long, source: ReviewSource): Review?

    fun findByExternalIdAndSource(externalId: String, source: ReviewSource): Review?
}
