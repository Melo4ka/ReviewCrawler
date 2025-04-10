package ru.digitalhoreca.reviewcrawler.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyCreateDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyDto
import ru.digitalhoreca.reviewcrawler.dto.company.CompanyUpdateDto
import ru.digitalhoreca.reviewcrawler.service.CompanyService

@RestController
@RequestMapping("/companies")
@Tag(name = "Company API", description = "API для управления компаниями")
class CompanyController(private val companyService: CompanyService) {

    @GetMapping
    @Operation(summary = "Получить все компании", description = "Возвращает список всех компаний")
    fun getAllCompanies() = ResponseEntity.ok(companyService.getAllCompanies())

    @PostMapping
    @Operation(summary = "Создать новую компанию", description = "Создает новую компанию с указанными данными")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Компания успешно создана"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса")
    )
    fun createCompany(@RequestBody companyCreateDto: CompanyCreateDto) =
        ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(companyCreateDto))

    @GetMapping("/{id}")
    @Operation(summary = "Получить компанию по ID", description = "Возвращает компанию по указанному ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Компания найдена"),
        ApiResponse(responseCode = "404", description = "Компания не найдена")
    )
    fun getCompanyById(
        @Parameter(description = "ID компании")
        @PathVariable id: Long
    ): ResponseEntity<CompanyDto> {
        val company = companyService.getCompanyById(id)
        return if (company != null) {
            ResponseEntity.ok(company)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить компанию", description = "Обновляет данные существующей компании")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Компания успешно обновлена"),
        ApiResponse(responseCode = "404", description = "Компания не найдена"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса")
    )
    fun updateCompany(
        @Parameter(description = "ID компании")
        @PathVariable id: Long,
        @RequestBody companyUpdateDto: CompanyUpdateDto
    ): ResponseEntity<CompanyDto> {
        val updatedCompany = companyService.updateCompany(id, companyUpdateDto)
        return if (updatedCompany != null) {
            ResponseEntity.ok(updatedCompany)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить компанию", description = "Удаляет компанию по указанному ID")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Компания успешно удалена"),
        ApiResponse(responseCode = "404", description = "Компания не найдена")
    )
    fun deleteCompany(
        @Parameter(description = "ID компании")
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        val deleted = companyService.deleteCompany(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search", params = ["name"])
    @Operation(summary = "Поиск компаний по названию", description = "Ищет компании по части названия")
    fun findCompaniesByName(
        @Parameter(description = "Название компании или его часть")
        @RequestParam name: String
    ) = ResponseEntity.ok(companyService.findCompaniesByName(name))

    @GetMapping("/search", params = ["name", "address"])
    @Operation(summary = "Поиск компаний по названию и адресу", description = "Ищет компании по названию и адресу")
    fun findCompaniesByNameAndAddress(
        @Parameter(description = "Название компании")
        @RequestParam name: String,
        @Parameter(description = "Адрес компании")
        @RequestParam address: String
    ) = ResponseEntity.ok(companyService.findCompanyByNameAndAddress(name, address))

    @GetMapping("/search", params = ["yandexId"])
    @Operation(
        summary = "Поиск компании по Yandex ID",
        description = "Ищет компанию по идентификатору в системе Яндекс"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Компания найдена"),
        ApiResponse(responseCode = "404", description = "Компания не найдена")
    )
    fun findCompanyByYandexId(
        @Parameter(description = "ID компании в Яндекс")
        @RequestParam yandexId: Long
    ): ResponseEntity<CompanyDto> {
        val company = companyService.findCompanyByYandexId(yandexId)
        return if (company != null) {
            ResponseEntity.ok(company)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/search", params = ["twoGisId"])
    @Operation(summary = "Поиск компании по 2GIS ID", description = "Ищет компанию по идентификатору в системе 2GIS")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Компания найдена"),
        ApiResponse(responseCode = "404", description = "Компания не найдена")
    )
    fun findCompanyByTwoGisId(
        @Parameter(description = "ID компании в 2GIS")
        @RequestParam twoGisId: Long
    ): ResponseEntity<CompanyDto> {
        val company = companyService.findCompanyByTwoGisId(twoGisId)
        return if (company != null) {
            ResponseEntity.ok(company)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}