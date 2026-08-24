package com.vertyll.veds.translation.infrastructure.persistence.repository

import com.vertyll.veds.translation.infrastructure.persistence.entity.LanguageJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationKeyJpaEntity
import com.vertyll.veds.translation.infrastructure.persistence.entity.TranslationValueJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional

@Repository
internal interface TranslationKeyJpaRepository : JpaRepository<TranslationKeyJpaEntity, String> {
    @Query(
        """
        SELECT k FROM TranslationKeyJpaEntity k
        WHERE (:sourceService IS NULL OR k.sourceService = :sourceService)
        AND (
            :searchTerm IS NULL
            OR LOWER(k.key) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
            OR LOWER(COALESCE(k.description, '')) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS string), '%'))
        )
        ORDER BY k.key
        """,
    )
    fun search(
        @Param("searchTerm") searchTerm: String?,
        @Param("sourceService") sourceService: String?,
        pageable: Pageable,
    ): Page<TranslationKeyJpaEntity>
}

@Repository
internal interface TranslationValueJpaRepository : JpaRepository<TranslationValueJpaEntity, java.util.UUID> {
    fun findByKeyAndLanguage(
        key: String,
        language: String,
    ): Optional<TranslationValueJpaEntity>

    fun findAllByKeyIn(keys: Collection<String>): List<TranslationValueJpaEntity>

    fun findAllByLanguage(language: String): List<TranslationValueJpaEntity>

    @Query("SELECT MAX(v.updatedAt) FROM TranslationValueJpaEntity v WHERE v.language = :language")
    fun latestUpdate(
        @Param("language") language: String,
    ): Instant?
}

@Repository
internal interface LanguageJpaRepository : JpaRepository<LanguageJpaEntity, String> {
    fun findByIsDefaultTrue(): Optional<LanguageJpaEntity>
}
