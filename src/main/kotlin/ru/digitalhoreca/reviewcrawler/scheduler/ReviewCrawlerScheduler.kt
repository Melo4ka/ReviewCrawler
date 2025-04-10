package ru.digitalhoreca.reviewcrawler.scheduler

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.digitalhoreca.reviewcrawler.crawler.ReviewCrawler
import ru.digitalhoreca.reviewcrawler.crawler.TwoGisReviewCrawler
import ru.digitalhoreca.reviewcrawler.crawler.YandexMapsReviewCrawler
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyDto
import ru.digitalhoreca.reviewcrawler.service.CompanyService

@Component
class ReviewCrawlerScheduler(
    private val companyService: CompanyService,
    private val twoGisReviewCrawler: TwoGisReviewCrawler,
    private val yandexMapsReviewCrawler: YandexMapsReviewCrawler
) {

    private val logger = LoggerFactory.getLogger(ReviewCrawlerScheduler::class.java)

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "crawlTwoGisReviews", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    fun crawlTwoGisReviews() = crawlReviews("2GIS", twoGisReviewCrawler, CompanyDto::twoGisId)

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "crawlYandexReviews", lockAtLeastFor = "5m", lockAtMostFor = "30m")
    fun crawlYandexReviews() = crawlReviews("YANDEX", yandexMapsReviewCrawler, CompanyDto::yandexId)

    private fun crawlReviews(name: String, crawler: ReviewCrawler, idGetter: (CompanyDto) -> Long?) {
        val companyIds = companyService.getAllCompanies()
            .filter { idGetter(it) != null }
            .mapNotNull { it.id }

        if (companyIds.isEmpty()) return

        logger.info("Запуск запланированного сбора отзывов $name для ${companyIds.size} компаний")
        crawler.crawlCompaniesReviews(companyIds)
        logger.info("Завершен запланированный сбор отзывов $name для ${companyIds.size} компаний")
    }
}
