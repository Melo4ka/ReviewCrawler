package ru.digitalhoreca.reviewcrawler.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.digitalhoreca.reviewcrawler.crawler.TwoGisReviewCrawler
import ru.digitalhoreca.reviewcrawler.crawler.YandexMapsReviewCrawler
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyCreateDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyUpdateDto
import ru.digitalhoreca.reviewcrawler.entity.Company
import ru.digitalhoreca.reviewcrawler.repository.CompanyRepository
import java.util.concurrent.Executors

@Service
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val twoGisReviewCrawler: TwoGisReviewCrawler,
    private val yandexMapsReviewCrawler: YandexMapsReviewCrawler
) {

    private val logger = LoggerFactory.getLogger(CompanyService::class.java)
    private val executor = Executors.newFixedThreadPool(2) {
        Thread(it, "company-review-crawler").apply {
            isDaemon = true
        }
    }

    fun getAllCompanies() = companyRepository.findAll().map { it.toDto() }

    fun getCompanyById(id: Long): CompanyDto? = companyRepository.findById(id).map { it.toDto() }.orElse(null)

    fun findCompaniesByName(name: String) = companyRepository.findByNameContainingIgnoreCase(name).map { it.toDto() }

    fun findCompanyByNameAndAddress(name: String, address: String) =
        companyRepository.findByNameContainingIgnoreCaseAndAddressContainingIgnoreCase(name, address).map { it.toDto() }

    fun findCompanyByYandexId(yandexId: Long) = companyRepository.findByYandexId(yandexId)?.toDto()

    fun findCompanyByTwoGisId(twoGisId: Long) = companyRepository.findByTwoGisId(twoGisId)?.toDto()

    fun createCompany(companyCreateDto: CompanyCreateDto): CompanyDto {
        val company = Company(
            name = companyCreateDto.name,
            address = companyCreateDto.address,
            yandexId = companyCreateDto.yandexId,
            twoGisId = companyCreateDto.twoGisId
        )
        val savedCompany = companyRepository.save(company).toDto()

        scheduleReviewCrawling(savedCompany.id)

        return savedCompany
    }

    @Transactional
    fun updateCompany(id: Long, companyUpdateDto: CompanyUpdateDto): CompanyDto? {
        val companyOptional = companyRepository.findById(id)
        if (companyOptional.isEmpty) {
            return null
        }

        val company = companyOptional.get()

        companyUpdateDto.name?.let { company.name = it }
        companyUpdateDto.address?.let { company.address = it }
        companyUpdateDto.yandexId?.let { company.yandexId = it }
        companyUpdateDto.twoGisId?.let { company.twoGisId = it }

        val updatedCompany = companyRepository.save(company).toDto()

        if (
            companyUpdateDto.yandexId != null && companyUpdateDto.yandexId != company.yandexId ||
            companyUpdateDto.twoGisId != null && companyUpdateDto.twoGisId != company.twoGisId
        ) {
            scheduleReviewCrawling(updatedCompany.id)
        }

        return updatedCompany
    }

    @Transactional
    fun deleteCompany(id: Long) =
        if (!companyRepository.existsById(id)) {
            false
        } else {
            companyRepository.deleteById(id)
            true
        }

    private fun scheduleReviewCrawling(companyId: Long?) {
        if (companyId == null) return

        logger.info("Запуск сбора отзывов для компании $companyId из всех источников")

        executor.submit {
            try {
                twoGisReviewCrawler.crawlCompanyReviews(companyId)
            } catch (e: Exception) {
                logger.error("Ошибка при сборе отзывов 2GIS: ${e.message}", e)
            }
        }

        executor.submit {
            try {
                yandexMapsReviewCrawler.crawlCompanyReviews(companyId)
            } catch (e: Exception) {
                logger.error("Ошибка при сборе отзывов YANDEX: ${e.message}", e)
            }
        }
    }

    private fun Company.toDto() = CompanyDto(
        id = this.id,
        name = this.name,
        address = this.address,
        yandexId = this.yandexId,
        twoGisId = this.twoGisId
    )
}
