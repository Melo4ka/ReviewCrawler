package ru.digitalhoreca.reviewcrawler.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyCreateDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyUpdateDto
import ru.digitalhoreca.reviewcrawler.service.CompanyService

@WebMvcTest(CompanyController::class)
class CompanyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var companyService: CompanyService

    @Test
    fun `getAllCompanies возвращает список компаний`() {
        val companies = listOf(
            createTestCompanyDto(1L),
            createTestCompanyDto(2L)
        )

        whenever(companyService.getAllCompanies()).thenReturn(companies)

        mockMvc.perform(get("/companies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(2))
    }

    @Test
    fun `getCompanyById возвращает компанию если она существует`() {
        val company = createTestCompanyDto(1L)
        whenever(companyService.getCompanyById(1L)).thenReturn(company)

        mockMvc.perform(get("/companies/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Company 1"))
    }

    @Test
    fun `getCompanyById возвращает 404 если компания не существует`() {
        whenever(companyService.getCompanyById(999L)).thenReturn(null)

        mockMvc.perform(get("/companies/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `createCompany создает новую компанию`() {
        val createDto = CompanyCreateDto(
            name = "New Company",
            address = "New Address",
            yandexId = 12345L,
            twoGisId = 67890L
        )

        val createdCompany = createTestCompanyDto(1L).copy(name = "New Company", address = "New Address")
        whenever(companyService.createCompany(any())).thenReturn(createdCompany)

        mockMvc.perform(
            post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Company"))
    }

    @Test
    fun `updateCompany обновляет существующую компанию`() {
        val updateDto = CompanyUpdateDto(
            name = "Updated Company",
            address = "Updated Address",
            yandexId = null,
            twoGisId = null
        )

        val updatedCompany = createTestCompanyDto(1L).copy(
            name = "Updated Company",
            address = "Updated Address"
        )

        whenever(companyService.updateCompany(any(), any())).thenReturn(updatedCompany)

        mockMvc.perform(
            put("/companies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Company"))
            .andExpect(jsonPath("$.address").value("Updated Address"))
    }

    @Test
    fun `updateCompany возвращает 404 если компания не существует`() {
        val updateDto = CompanyUpdateDto(
            name = "Updated Company",
            address = "Updated Address",
            yandexId = null,
            twoGisId = null
        )

        whenever(companyService.updateCompany(any(), any())).thenReturn(null)

        mockMvc.perform(
            put("/companies/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `deleteCompany возвращает 204 при успешном удалении`() {
        whenever(companyService.deleteCompany(1L)).thenReturn(true)

        mockMvc.perform(delete("/companies/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `deleteCompany возвращает 404 если компания не существует`() {
        whenever(companyService.deleteCompany(999L)).thenReturn(false)

        mockMvc.perform(delete("/companies/999"))
            .andExpect(status().isNotFound)
    }

    private fun createTestCompanyDto(id: Long) = CompanyDto(
        id = id,
        name = "Test Company $id",
        address = "Test Address $id",
        yandexId = id * 1000L,
        twoGisId = id * 2000L
    )
} 