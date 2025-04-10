package ru.digitalhoreca.reviewcrawler.integration

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
import ru.digitalhoreca.reviewcrawler.controller.CompanyController
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyCreateDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyUpdateDto
import ru.digitalhoreca.reviewcrawler.service.CompanyService

@WebMvcTest(CompanyController::class)
class CompanyApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var companyService: CompanyService

    @Test
    fun `API может создавать, получать, обновлять и удалять компании`() {
        val companyDto = CompanyDto(
            id = 1L,
            name = "Тестовый Ресторан",
            address = "г. Москва, ул. Тестовая, д. 123",
            yandexId = 12345L,
            twoGisId = 67890L
        )

        val updatedCompanyDto = companyDto.copy(name = "Обновленный Ресторан")

        whenever(companyService.createCompany(any())).thenReturn(companyDto)
        whenever(companyService.getCompanyById(1L)).thenReturn(companyDto)
        whenever(companyService.updateCompany(any(), any())).thenReturn(updatedCompanyDto)
        whenever(companyService.deleteCompany(1L)).thenReturn(true)
        whenever(companyService.findCompaniesByName("Обновленный")).thenReturn(listOf(updatedCompanyDto))

        val createDto = CompanyCreateDto(
            name = "Тестовый Ресторан",
            address = "г. Москва, ул. Тестовая, д. 123",
            yandexId = 12345L,
            twoGisId = 67890L
        )

        mockMvc.perform(
            post("/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Тестовый Ресторан"))
            .andExpect(jsonPath("$.address").value("г. Москва, ул. Тестовая, д. 123"))
            .andExpect(jsonPath("$.yandexId").value(12345))
            .andExpect(jsonPath("$.twoGisId").value(67890))

        mockMvc.perform(get("/companies/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Тестовый Ресторан"))
            .andExpect(jsonPath("$.address").value("г. Москва, ул. Тестовая, д. 123"))

        val updateDto = CompanyUpdateDto(
            name = "Обновленный Ресторан",
            address = null,
            yandexId = null,
            twoGisId = null
        )

        mockMvc.perform(
            put("/companies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Обновленный Ресторан"))

        mockMvc.perform(get("/companies/search?name=Обновленный"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Обновленный Ресторан"))

        mockMvc.perform(delete("/companies/1"))
            .andExpect(status().isNoContent)

        whenever(companyService.getCompanyById(1L)).thenReturn(null)

        mockMvc.perform(get("/companies/1"))
            .andExpect(status().isNotFound)
    }
} 