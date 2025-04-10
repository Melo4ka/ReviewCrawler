package ru.digitalhoreca.reviewcrawler.entity

import jakarta.persistence.*

@Entity
@Table(name = "companies")
data class Company(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var address: String,

    @Column(name = "yandex_id")
    var yandexId: Long? = null,

    @Column(name = "two_gis_id")
    var twoGisId: Long? = null,

    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reviews: MutableList<Review> = mutableListOf()
)
