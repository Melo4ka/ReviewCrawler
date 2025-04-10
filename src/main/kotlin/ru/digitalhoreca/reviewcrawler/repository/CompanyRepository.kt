package ru.digitalhoreca.reviewcrawler.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.digitalhoreca.reviewcrawler.entity.Company

interface CompanyRepository : JpaRepository<Company, Long> {

    fun findByYandexId(yandexId: Long?): Company?

    fun findByTwoGisId(twoGisId: Long?): Company?

    fun findByNameContainingIgnoreCase(name: String): List<Company>

    fun findByNameContainingIgnoreCaseAndAddressContainingIgnoreCase(name: String, address: String): List<Company>
}
