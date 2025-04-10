package ru.digitalhoreca.reviewcrawler.service

import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.digitalhoreca.reviewcrawler.crawler.TwoGisReviewCrawler
import ru.digitalhoreca.reviewcrawler.crawler.YandexMapsReviewCrawler
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyCreateDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyUpdateDto
import ru.digitalhoreca.reviewcrawler.entity.Company
import ru.digitalhoreca.reviewcrawler.repository.CompanyRepository
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompanyServiceTest {

    private lateinit var companyRepository: CompanyRepository
    private lateinit var twoGisReviewCrawler: TwoGisReviewCrawler
    private lateinit var yandexMapsReviewCrawler: YandexMapsReviewCrawler
    private lateinit var companyService: CompanyService

    @BeforeEach
    fun setUp() {
        companyRepository = mockk()
        twoGisReviewCrawler = mockk(relaxed = true)
        yandexMapsReviewCrawler = mockk(relaxed = true)

        companyService = CompanyService(companyRepository, twoGisReviewCrawler, yandexMapsReviewCrawler)
    }

    @Test
    fun `getAllCompanies возвращает все компании`() {
        val companies = listOf(
            createTestCompany(1L, "Company 1"),
            createTestCompany(2L, "Company 2")
        )

        every { companyRepository.findAll() } returns companies

        val result = companyService.getAllCompanies()

        assertEquals(2, result.size)
        assertEquals("Company 1", result[0].name)
        assertEquals("Company 2", result[1].name)

        verify { companyRepository.findAll() }
    }

    @Test
    fun `getCompanyById возвращает компанию по ID`() {
        val company = createTestCompany(1L, "Test Company")
        every { companyRepository.findById(1L) } returns Optional.of(company)

        val result = companyService.getCompanyById(1L)

        assertEquals("Test Company", result?.name)
        assertEquals(1L, result?.id)

        verify { companyRepository.findById(1L) }
    }

    @Test
    fun `getCompanyById возвращает null если компания не найдена`() {
        every { companyRepository.findById(999L) } returns Optional.empty()

        val result = companyService.getCompanyById(999L)

        assertNull(result)

        verify { companyRepository.findById(999L) }
    }

    @Test
    fun `createCompany сохраняет новую компанию и запускает сбор отзывов`() {
        val createDto = CompanyCreateDto(
            name = "New Company",
            address = "New Address",
            yandexId = 123L,
            twoGisId = 456L
        )

        val savedCompany = createTestCompany(
            id = 1L,
            name = "New Company",
            address = "New Address",
            yandexId = 123L,
            twoGisId = 456L
        )

        every { companyRepository.save(any()) } returns savedCompany

        val result = companyService.createCompany(createDto)

        assertEquals("New Company", result.name)
        assertEquals("New Address", result.address)
        assertEquals(123L, result.yandexId)
        assertEquals(456L, result.twoGisId)

        verify { companyRepository.save(any()) }
    }

    @Test
    fun `updateCompany обновляет существующую компанию`() {
        val company = createTestCompany(1L, "Old Name", "Old Address")
        val updateDto = CompanyUpdateDto(
            name = "Updated Name",
            address = "Updated Address",
            yandexId = null,
            twoGisId = null
        )

        val updatedCompany = createTestCompany(1L, "Updated Name", "Updated Address")

        every { companyRepository.findById(1L) } returns Optional.of(company)
        every { companyRepository.save(any()) } returns updatedCompany

        val result = companyService.updateCompany(1L, updateDto)

        assertEquals("Updated Name", result?.name)
        assertEquals("Updated Address", result?.address)

        verify { companyRepository.findById(1L) }
        verify { companyRepository.save(any()) }
    }

    @Test
    fun `updateCompany возвращает null если компания не найдена`() {
        val updateDto = CompanyUpdateDto(
            name = "Updated Name",
            address = "Test Address",
            yandexId = null,
            twoGisId = null
        )

        every { companyRepository.findById(999L) } returns Optional.empty()

        val result = companyService.updateCompany(999L, updateDto)

        assertNull(result)

        verify { companyRepository.findById(999L) }
        verify(exactly = 0) { companyRepository.save(any()) }
    }

    @Test
    fun `deleteCompany удаляет существующую компанию`() {
        every { companyRepository.existsById(1L) } returns true
        every { companyRepository.deleteById(1L) } just runs

        val result = companyService.deleteCompany(1L)

        assertTrue(result)

        verify { companyRepository.existsById(1L) }
        verify { companyRepository.deleteById(1L) }
    }

    @Test
    fun `deleteCompany возвращает false если компания не найдена`() {
        every { companyRepository.existsById(999L) } returns false

        val result = companyService.deleteCompany(999L)

        assertFalse(result)

        verify { companyRepository.existsById(999L) }
        verify(exactly = 0) { companyRepository.deleteById(any()) }
    }

    private fun createTestCompany(
        id: Long,
        name: String,
        address: String = "Test Address",
        yandexId: Long? = null,
        twoGisId: Long? = null
    ) = Company(
        id = id,
        name = name,
        address = address,
        yandexId = yandexId,
        twoGisId = twoGisId
    )
} 