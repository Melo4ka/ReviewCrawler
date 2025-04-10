package ru.digitalhoreca.reviewcrawler.crawler

import com.fasterxml.jackson.databind.ObjectMapper
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.devtools.v135.network.Network
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.digitalhoreca.reviewcrawler.dto.review.ReviewCreateDto
import ru.digitalhoreca.reviewcrawler.entity.Company
import ru.digitalhoreca.reviewcrawler.entity.ReviewSource
import ru.digitalhoreca.reviewcrawler.service.CompanyService
import ru.digitalhoreca.reviewcrawler.service.ReviewService
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.Collections.synchronizedList

@Component
class YandexMapsReviewCrawler(
    @Lazy private val companyService: CompanyService,
    private val reviewService: ReviewService,
    private val objectMapper: ObjectMapper,
    private val chromeOptions: ChromeOptions
) : ReviewCrawler {

    private val logger = LoggerFactory.getLogger(YandexMapsReviewCrawler::class.java)

    private companion object {
        private const val REVIEW_URL = "https://yandex.ru/maps/org/%s/reviews/"
        private const val API_URL_PATTERN = "/maps/api/business/fetchReviews"
        private const val PAGE_LOAD_TIMEOUT_SECONDS = 30L
        private const val SCROLL_INTERVAL_MILLIS = 1000L
        private const val SCROLL_WAIT_TIME_MS = 2000L
        private const val MAX_NO_NEW_REVIEWS_ATTEMPTS = 3
    }

    override fun crawlCompaniesReviews(companyIds: List<Long>) {
        if (companyIds.isEmpty()) return

        logger.info("Начинаю сбор отзывов Яндекс.Карты для ${companyIds.size} компаний")

        val companies = companyIds.mapNotNull { companyId ->
            val companyDto = companyService.getCompanyById(companyId)
            if (companyDto?.yandexId == null) {
                logger.warn("Компания с ID $companyId не имеет настроенного Yandex ID")
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
            logger.warn("Нет компаний с настроенным Yandex ID для обработки")
            return
        }

        companies.forEach { company ->
            processCompanyReviews(company)
        }

        logger.info("Завершен сбор отзывов Яндекс.Карты для ${companies.size} компаний")
    }

    private fun processCompanyReviews(company: Company) {
        val companyId = company.id ?: return
        val yandexId = company.yandexId ?: return

        logger.info("Начинаю сбор отзывов Яндекс.Карты для компании ID: $companyId, Yandex ID: $yandexId")

        val driver = ChromeDriver(chromeOptions)
        val reviews = synchronizedList(mutableListOf<String>())

        driver.devTools.createSession()
        driver.devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()))
        driver.devTools.addListener(Network.responseReceived()) { responseReceivedEvent ->
            val response = responseReceivedEvent.response
            val url = response.url

            if (url.contains(API_URL_PATTERN)) {
                logger.debug("Перехвачен ответ от API: $url")

                try {
                    Thread.sleep(1000)
                    val responseBody = driver.devTools.send(Network.getResponseBody(responseReceivedEvent.requestId))
                    reviews += responseBody.body
                    logger.debug("Получено тело ответа длиной ${responseBody.body}")
                } catch (e: Exception) {
                    logger.error("Ошибка при получении тела ответа: ${e.message}", e)
                }
            }
        }

        try {
            val url = REVIEW_URL.format(yandexId)
            logger.info("Открываю страницу Яндекс.Карты: $url")
            driver.get(url)

            val wait = WebDriverWait(driver, Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SECONDS))
            logger.debug("Ожидаю загрузки страницы и элементов...")

            Thread.sleep(2000)

            try {
                val sortSelector =
                    wait.until(ExpectedConditions.elementToBeClickable(By.className("rating-ranking-view")))
                sortSelector.click()
                logger.info("Нажат селектор сортировки отзывов")

                val sortMenu =
                    wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("rating-ranking-view__popup")))

                val sortByNewestOption = sortMenu.findElements(By.className("rating-ranking-view__popup-line"))
                    .find { it.getAttribute("aria-label") == "По новизне" }

                if (sortByNewestOption != null) {
                    sortByNewestOption.click()
                    logger.info("Выбрана сортировка отзывов 'По новизне'")

                    Thread.sleep(2000)
                } else {
                    logger.warn("Не удалось найти опцию сортировки 'По новизне'")
                }
            } catch (e: Exception) {
                logger.error("Ошибка при изменении сортировки отзывов: ${e.message}", e)
            }

            var scrollElement: WebElement? = null

            try {
                scrollElement =
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.className("card-reviews-view")))
            } catch (e: Exception) {
                logger.warn("Элемент скроллинга не найден по основному селектору: ${e.message}")
            }

            val lastReview = reviewService.findLastReviewByCompanyIdAndSource(companyId, ReviewSource.YANDEX_MAPS)
            logger.info("Последний сохраненный отзыв: ${lastReview?.externalId ?: "не найден"}")

            val newReviews = mutableListOf<ReviewCreateDto>()
            val processedRequests = mutableSetOf<String>()
            var consecutiveNoNewReviewAttempts = 0
            var foundExistingReview = false

            while (consecutiveNoNewReviewAttempts < MAX_NO_NEW_REVIEWS_ATTEMPTS) {
                try {
                    if (scrollElement != null) {
                        try {
                            scrollElement.click()
                            logger.debug("Успешно кликнули по элементу скроллинга")
                        } catch (e: Exception) {
                            logger.warn("Не удалось кликнуть по элементу скроллинга: ${e.message}")
                        }
                        Thread.sleep(500)

                        try {
                            driver.findElement(By.tagName("body")).sendKeys(Keys.END)
                            logger.debug("Отправлена команда Keys.END элементу body")
                        } catch (e: Exception) {
                            logger.warn("Не удалось отправить Keys.END элементу body: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Не удалось выполнить скроллинг: ${e.message}")
                }

                Thread.sleep(SCROLL_INTERVAL_MILLIS)

                val networkRequests = reviews.toList()
                reviews.clear()

                if (networkRequests.isNotEmpty()) {
                    logger.debug("Найдено ${networkRequests.size} новых сетевых запросов")

                    val newRequests = networkRequests.filter { !processedRequests.contains(it) }

                    if (newRequests.isNotEmpty()) {
                        logger.debug("Получено ${newRequests.size} новых запросов к API")

                        for (requestBody in newRequests) {
                            processedRequests.add(requestBody)

                            val parsedReviews = parseYandexReviews(requestBody, companyId)
                            logger.debug("Распарсено ${parsedReviews.size} отзывов из запроса")

                            for (review in parsedReviews) {
                                if (lastReview != null && review.externalId == lastReview.externalId) {
                                    logger.info("Найден последний сохраненный отзыв с ID: ${lastReview.externalId}")
                                    foundExistingReview = true
                                    break
                                }

                                if (newReviews.none { it.externalId == review.externalId } &&
                                    !reviewService.existsByExternalIdAndSource(
                                        review.externalId,
                                        ReviewSource.YANDEX_MAPS
                                    )) {
                                    newReviews.add(review)
                                    logger.debug("Добавлен новый отзыв с ID: ${review.externalId}")
                                }
                            }

                            if (foundExistingReview) break
                        }

                        consecutiveNoNewReviewAttempts = 0
                    } else {
                        consecutiveNoNewReviewAttempts++
                        logger.debug("Не найдено новых запросов к API, попытка $consecutiveNoNewReviewAttempts из $MAX_NO_NEW_REVIEWS_ATTEMPTS")
                    }
                } else {
                    consecutiveNoNewReviewAttempts++
                    logger.debug("Нет сетевых запросов, попытка $consecutiveNoNewReviewAttempts из $MAX_NO_NEW_REVIEWS_ATTEMPTS")
                }

                Thread.sleep(SCROLL_WAIT_TIME_MS)

                if (foundExistingReview) {
                    logger.info("Найден существующий отзыв, прекращаем сбор")
                    break
                }
            }

            if (newReviews.isNotEmpty()) {
                logger.info("Найдено ${newReviews.size} новых отзывов для компании ID: $companyId")

                val sortedReviews = newReviews.sortedBy { it.date }

                sortedReviews.forEach { reviewDto ->
                    try {
                        reviewService.saveReview(reviewDto)
                        logger.info("Сохранен отзыв Яндекс.Карты с externalId: ${reviewDto.externalId}, дата: ${reviewDto.date}")
                    } catch (e: Exception) {
                        logger.error(
                            "Ошибка при сохранении отзыва Яндекс.Карты с externalId: ${reviewDto.externalId}",
                            e
                        )
                    }
                }

                logger.info("Сохранено ${sortedReviews.size} новых отзывов для компании ID: $companyId")
            } else {
                logger.info("Новых отзывов для компании ID: $companyId не найдено")
            }
        } catch (e: Exception) {
            logger.error("Ошибка при сборе отзывов Яндекс.Карты для компании ID: $companyId: ${e.message}", e)
        } finally {
            driver.devTools.clearListeners()
            driver.devTools.send(Network.disable())
            driver.devTools.disconnectSession()
            driver.quit()
        }
    }

    private fun parseYandexReviews(responseBody: String, companyId: Long): List<ReviewCreateDto> {
        val reviews = mutableListOf<ReviewCreateDto>()

        try {
            val rootNode = objectMapper.readTree(responseBody)
            val reviewsNode = rootNode.path("data").path("reviews")

            if (reviewsNode.isArray) {
                logger.debug("Найдено ${reviewsNode.size()} отзывов в JSON")

                for (reviewNode in reviewsNode) {
                    try {
                        val id = reviewNode.path("reviewId").asText()
                        val rating = reviewNode.path("rating").asInt().toFloat()
                        val text = reviewNode.path("text").asText("")
                        val authorName = reviewNode.path("author").path("name").asText()
                        val dateStr = reviewNode.path("updatedTime").asText()
                        val dateTime = Date.from(ZonedDateTime.parse(dateStr).toInstant())

                        val imageUrls = mutableListOf<String>()
                        val photosNode = reviewNode.path("photos")

                        if (photosNode.isArray) {
                            for (photoNode in photosNode) {
                                val photoUrl = photoNode.path("urlTemplate").asText().replace("{size}", "orig")
                                if (photoUrl.isNotEmpty()) {
                                    imageUrls.add(photoUrl)
                                }
                            }
                        }

                        logger.debug("Обработан отзыв: id=$id, автор=$authorName, рейтинг=$rating, дата=$dateTime")

                        val review = ReviewCreateDto(
                            companyId = companyId,
                            rating = rating,
                            text = text,
                            authorName = authorName,
                            date = dateTime,
                            source = ReviewSource.YANDEX_MAPS,
                            externalId = id,
                            imageUrls = imageUrls
                        )

                        reviews.add(review)
                    } catch (e: Exception) {
                        logger.error("Ошибка при парсинге отдельного отзыва: ${e.message}", e)
                    }
                }
            } else {
                logger.warn("JSON не содержит массива отзывов или имеет неизвестную структуру")
                logger.debug("Структура JSON: $responseBody")
            }
        } catch (e: Exception) {
            logger.error("Ошибка при парсинге ответа API: ${e.message}", e)
        }

        return reviews
    }
}