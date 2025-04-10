package ru.digitalhoreca.reviewcrawler.crawler

interface ReviewCrawler {

    fun crawlCompaniesReviews(companyIds: List<Long>)

    fun crawlCompanyReviews(companyId: Long) = crawlCompaniesReviews(listOf(companyId))
}

