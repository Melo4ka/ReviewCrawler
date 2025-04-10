package ru.digitalhoreca.reviewcrawler.crawler

import com.fasterxml.jackson.databind.ObjectMapper
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.devtools.v135.network.Network
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import ru.digitalhoreca.reviewcrawler.dto.review.ReviewCreateDto
import ru.digitalhoreca.reviewcrawler.entity.Company
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import ru.digitalhoreca.reviewcrawler.service.CompanyService
import ru.digitalhoreca.reviewcrawler.service.ReviewService
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

@Component
class TwoGisReviewCrawler(
    @Lazy private val companyService: CompanyService,
    private val reviewService: ReviewService,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val chromeOptions: ChromeOptions
) : ReviewCrawler {

    private val logger = LoggerFactory.getLogger(TwoGisReviewCrawler::class.java)

    private companion object {
        private const val REVIEW_URL = "https://2gis.ru/spb/firm/%s/tab/reviews"
        private const val API_URL_PATTERN = "https://public-api.reviews.2gis.com/2.0/branches/"
        private const val API_URL_TEMPLATE =
            "https://public-api.reviews.2gis.com/2.0/branches/%s/reviews?limit=50&offset=%s&key=%s"
        private const val REQUEST_LIMIT = 50
        private const val PAGE_LOAD_TIMEOUT_SECONDS = 20L
        private const val AJAX_WAIT_TIMEOUT_SECONDS = 5L
        private const val MAX_API_KEY_ATTEMPTS = 5
    }

    override fun crawlCompaniesReviews(companyIds: List<Long>) {
        if (companyIds.isEmpty()) return

        logger.info("Начинаю сбор отзывов 2GIS для ${companyIds.size} компаний")

        val companies = companyIds.mapNotNull {
            val companyDto = companyService.getCompanyById(it)
            if (companyDto?.twoGisId == null) {
                logger.warn("Компания с ID $it не имеет настроенного 2GIS ID")
                null
            } else {
                Company(
                    id = companyDto.id,
                    name = companyDto.name,
                    address = companyDto.address,
                    yandexId = companyDto.yandexId,
                    twoGisId = companyDto.twoGisId
                )
            }
        }

        if (companies.isEmpty()) {
            logger.warn("Нет компаний с настроенным 2GIS ID для обработки")
            return
        }

        val apiKey = getApiKey(companies.first().twoGisId.toString())

        if (apiKey == null) {
            logger.error("Не удалось получить API ключ 2GIS. Сбор отзывов невозможен.")
            return
        }

        companies.forEach { processCompanyReviews(it, apiKey) }

        logger.info("Завершен сбор отзывов 2GIS для ${companies.size} компаний")
    }

    private fun processCompanyReviews(company: Company, apiKey: String) {
        val companyId = company.id ?: return
        val twoGisId = company.twoGisId ?: return

        logger.info("Начинаю сбор отзывов 2GIS для компании ID: $companyId, 2GIS ID: $twoGisId")

        try {
            val lastReview = reviewService.findLastReviewByCompanyIdAndSource(companyId, ReviewSource.TWO_GIS)
            var offset = 0
            val newReviews = mutableListOf<ReviewCreateDto>()

            reviewsWhile@ while (true) {
                val apiUrl = API_URL_TEMPLATE.format(twoGisId, offset, apiKey)
                val response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String::class.java
                )

                val responseBody = response.body
                if (responseBody.isNullOrEmpty()) {
                    logger.warn("Получен пустой ответ от API 2GIS для компании $companyId")
                    break
                }

                val parsedReviews = parseReviews(responseBody, companyId)

                if (parsedReviews.isEmpty()) {
                    logger.info("Больше нет доступных отзывов для компании $companyId")
                    break
                }

                for (review in parsedReviews) {
                    if (lastReview != null && review.externalId == lastReview.externalId) {
                        logger.info("Найден существующий отзыв, завершаем сбор для компании $companyId")
                        break@reviewsWhile
                    }

                    if (newReviews.none { it.externalId == review.externalId } &&
                        !reviewService.existsByExternalIdAndSource(review.externalId, ReviewSource.TWO_GIS)) {
                        newReviews.add(review)
                    }
                }

                offset += REQUEST_LIMIT
            }

            if (newReviews.isNotEmpty()) {
                logger.info("Найдено ${newReviews.size} новых отзывов для компании ID: $companyId")

                val sortedReviews = newReviews.sortedBy { it.date }

                sortedReviews.forEach { reviewDto ->
                    try {
                        reviewService.saveReview(reviewDto)
                        logger.info("Сохранен отзыв 2GIS с externalId: ${reviewDto.externalId}, дата: ${reviewDto.date}")
                    } catch (e: Exception) {
                        logger.error("Ошибка при сохранении отзыва 2GIS с externalId: ${reviewDto.externalId}", e)
                    }
                }

                logger.info("Сохранено ${sortedReviews.size} новых отзывов для компании ID: $companyId")
            } else {
                logger.info("Новых отзывов для компании ID: $companyId не найдено")
            }

        } catch (e: Exception) {
            logger.error("Ошибка при сборе отзывов 2GIS для компании ID: $companyId: ${e.message}", e)
        }
    }

    private fun getApiKey(twoGisId: String): String? {
        val driver = ChromeDriver(chromeOptions)
        val apiKey = AtomicReference<String>()

        try {
            driver.devTools.createSession()
            driver.devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()))

            driver.devTools.addListener(Network.requestWillBeSent()) { requestEvent ->
                val request = requestEvent.request
                val url = request.url

                if (url.contains(API_URL_PATTERN) && url.contains("/reviews") && url.contains("key=")) {
                    logger.debug("Перехвачен запрос к API: $url")

                    val pattern = Pattern.compile("$API_URL_PATTERN.*?key=([a-f0-9-]+)")
                    val matcher = pattern.matcher(url)

                    if (matcher.find()) {
                        apiKey.set(matcher.group(1))
                        logger.info("Обнаружен API ключ: ${apiKey.get()}")
                    }
                }
            }

            val url = REVIEW_URL.format(twoGisId)
            logger.info("Открываю страницу 2GIS для получения API ключа")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS))
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[data-scroll='true']")))

            var attempts = 0
            while (apiKey.get() == null && attempts < MAX_API_KEY_ATTEMPTS) {
                Thread.sleep(AJAX_WAIT_TIMEOUT_SECONDS * 1000)
                attempts++
            }

            return apiKey.get()
        } finally {
            try {
                driver.devTools.clearListeners()
                driver.devTools.send(Network.disable())
                driver.devTools.disconnectSession()
                driver.quit()
            } catch (e: Exception) {
                logger.warn("Ошибка при закрытии драйвера: ${e.message}")
            }
        }
    }

    private fun parseReviews(responseBody: String, companyId: Long): List<ReviewCreateDto> {
        val reviews = mutableListOf<ReviewCreateDto>()

        try {
            val rootNode = objectMapper.readTree(responseBody)
            val reviewsNode = rootNode.path("reviews")

            if (!reviewsNode.isArray) {
                return reviews
            }

            for (reviewNode in reviewsNode) {
                try {
                    val id = reviewNode.path("id").asText()
                    val rating = reviewNode.path("rating").asDouble().toFloat()
                    val text = reviewNode.path("text").asText("")
                    val dateStr = reviewNode.path("date_created").asText()
                    val authorName = reviewNode.path("user").path("name").asText()
                    val dateTime = Date.from(ZonedDateTime.parse(dateStr).toInstant())

                    val imageUrls = mutableListOf<String>()
                    val imagesArray = reviewNode.path("photos")
                    for (imageNode in imagesArray) {
                        val previewUrls = imageNode.path("preview_urls")

                        val regularUrl = previewUrls.path("url").asText()
                        if (regularUrl.isNotEmpty()) {
                            imageUrls.add(regularUrl)
                        }
                    }

                    val review = ReviewCreateDto(
                        companyId = companyId,
                        rating = rating,
                        text = text,
                        authorName = authorName,
                        date = dateTime,
                        source = ReviewSource.TWO_GIS,
                        externalId = id,
                        imageUrls = imageUrls
                    )

                    reviews.add(review)
                } catch (e: Exception) {
                    logger.error("Ошибка при парсинге отдельного отзыва: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка при парсинге ответа API: ${e.message}", e)
        }

        return reviews
    }
} 