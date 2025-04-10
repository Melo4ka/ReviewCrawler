package ru.digitalhoreca.reviewcrawler.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.digitalhoreca.reviewcrawler.dto.review.ReviewDto
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import ru.digitalhoreca.reviewcrawler.service.ReviewService

@RestController
@RequestMapping("/reviews")
@Tag(name = "Review API", description = "API для управления отзывами")
class ReviewController(private val reviewService: ReviewService) {

    @GetMapping
    @Operation(
        summary = "Получить отзывы",
        description = "Возвращает отзывы компаний с возможностью фильтрации по источнику и пагинацией"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Отзывы успешно получены"),
        ApiResponse(responseCode = "400", description = "Не указаны ID компаний")
    )
    fun getReviews(
        @Parameter(description = "ID компаний для которых нужно получить отзывы", required = true)
        @RequestParam(name = "companyId") companyIds: List<Long>,

        @Parameter(description = "Источник отзывов (YANDEX_MAPS, TWO_GIS)", required = false)
        @RequestParam(required = false) source: ReviewSource?,

        @Parameter(description = "Параметры пагинации", example = "{\"page\": 0, \"size\": 20}")
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ReviewDto>> {
        if (companyIds.isEmpty()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(reviewService.getReviewsByCompanyIds(companyIds, source, pageable))
    }
}
