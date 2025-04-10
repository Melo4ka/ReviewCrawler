package ru.digitalhoreca.reviewcrawler.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.digitalhoreca.reviewcrawler.crawler.TwoGisReviewCrawler
import ru.digitalhoreca.reviewcrawler.crawler.YandexMapsReviewCrawler

@RestController
@RequestMapping("/crawler")
@Tag(name = "Crawler API", description = "API для управления сбором отзывов")
class CrawlerController(
    private val twoGisReviewCrawler: TwoGisReviewCrawler,
    private val yandexMapsReviewCrawler: YandexMapsReviewCrawler
) {

    @PostMapping("/twogis")
    @Operation(
        summary = "Запустить сбор отзывов из 2GIS",
        description = "Запускает сбор отзывов для указанных компаний из 2GIS"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Сбор отзывов запущен успешно"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса")
    )
    fun crawlTwoGisReviews(
        @Parameter(
            description = "ID компаний для сбора отзывов",
            required = true,
            example = "[1, 2, 3]"
        )
        @RequestParam(name = "companyId") companyIds: List<Long>
    ) {
        twoGisReviewCrawler.crawlCompaniesReviews(companyIds)
    }

    @PostMapping("/yandex")
    @Operation(
        summary = "Запустить сбор отзывов из Яндекс",
        description = "Запускает сбор отзывов для указанных компаний из Яндекс.Карт"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Сбор отзывов запущен успешно"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса")
    )
    fun crawlYandexReviews(
        @Parameter(
            description = "ID компаний для сбора отзывов",
            required = true,
            example = "[1, 2, 3]"
        )
        @RequestParam(name = "companyId") companyIds: List<Long>
    ) {
        yandexMapsReviewCrawler.crawlCompaniesReviews(companyIds)
    }
}
