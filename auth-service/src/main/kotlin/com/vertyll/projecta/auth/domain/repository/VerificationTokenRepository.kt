package com.vertyll.projecta.auth.domain.repository

import com.vertyll.projecta.auth.domain.model.VerificationToken
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VerificationTokenRepository : JpaRepository<VerificationToken, Long> {
    fun findByToken(token: String): Optional<VerificationToken>

    fun findByUsernameAndTokenType(username: String, tokenType: String): Optional<VerificationToken>

    fun findByAdditionalData(additionalData: String): Optional<VerificationToken>
}
