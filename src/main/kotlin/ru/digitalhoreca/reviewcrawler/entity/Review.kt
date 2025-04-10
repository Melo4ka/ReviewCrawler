package ru.digitalhoreca.reviewcrawler.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "reviews")
data class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    var company: Company,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: ReviewSource,

    @Column(name = "external_id", nullable = false)
    var externalId: String,

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    var text: String,

    @Column(name = "author_name", nullable = false)
    var authorName: String,

    @Column(nullable = false)
    var rating: Float,

    @Column(nullable = false)
    var date: Date,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "review_images",
        joinColumns = [JoinColumn(name = "review_id")]
    )
    @Column(name = "image_url")
    var imageUrls: MutableList<String> = mutableListOf()
)
