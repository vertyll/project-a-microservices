package com.vertyll.projecta.auth.domain.service

import com.vertyll.projecta.sharedinfrastructure.config.JwtConstants
import com.vertyll.projecta.sharedinfrastructure.config.SharedConfigProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    private val sharedConfig: SharedConfigProperties
) {
    private val secretKey: String
        get() = sharedConfig.security.jwt.secretKey

    private val accessTokenExpiration: Long
        get() = sharedConfig.security.jwt.accessTokenExpiration

    private val refreshTokenExpiration: Long
        get() = sharedConfig.security.jwt.refreshTokenExpiration

    private val refreshTokenCookieName: String
        get() = sharedConfig.security.jwt.refreshTokenCookieName

    fun extractUsername(token: String): String {
        return extractClaim(token) { it.subject }
    }

    fun generateToken(userDetails: UserDetails): String {
        // Create claims including roles for the token
        val extraClaims = mutableMapOf<String, Any>()
        // Add user roles to token
        val roles = userDetails.authorities.map { it.authority }.toList()
        extraClaims[JwtConstants.CLAIM_ROLES] = roles
        extraClaims[JwtConstants.CLAIM_TOKEN_ID] = UUID.randomUUID().toString()
        extraClaims[JwtConstants.CLAIM_TYPE] = JwtConstants.TOKEN_TYPE_ACCESS

        return generateToken(extraClaims, userDetails)
    }

    fun generateToken(extraClaims: Map<String, Any>, userDetails: UserDetails): String {
        val now = Instant.now()
        return Jwts.builder()
            .claims()
            .add(extraClaims)
            .subject(userDetails.username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
            .and()
            .signWith(getSigningKey())
            .compact()
    }

    fun generateRefreshToken(userDetails: UserDetails): String {
        val extraClaims = mutableMapOf<String, Any>()
        extraClaims[JwtConstants.CLAIM_TYPE] = JwtConstants.TOKEN_TYPE_REFRESH
        extraClaims[JwtConstants.CLAIM_TOKEN_ID] = UUID.randomUUID().toString()

        // Add user roles to token
        val roles = userDetails.authorities.map { it.authority }.toList()
        extraClaims[JwtConstants.CLAIM_ROLES] = roles

        return generateRefreshToken(extraClaims, userDetails)
    }

    fun generateRefreshToken(extraClaims: Map<String, Any>, userDetails: UserDetails): String {
        val now = Instant.now()
        return Jwts.builder()
            .claims()
            .add(extraClaims)
            .subject(userDetails.username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(refreshTokenExpiration)))
            .id(UUID.randomUUID().toString()) // Add a unique ID to each token
            .and()
            .signWith(getSigningKey())
            .compact()
    }

    fun getRefreshTokenCookieNameFromConfig(): String {
        return refreshTokenCookieName
    }

    fun getRefreshTokenExpirationTime(): Long {
        return refreshTokenExpiration
    }

    fun getAccessTokenExpirationTime(): Long {
        return accessTokenExpiration
    }

    fun isTokenValid(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        return username == userDetails.username && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date.from(Instant.now()))
    }

    private fun extractExpiration(token: String): Date {
        return extractClaim(token) { it.expiration }
    }

    fun extractRoles(token: String): List<String> {
        return extractClaim(token) { claims ->
            @Suppress("UNCHECKED_CAST")
            claims[JwtConstants.CLAIM_ROLES] as? List<String> ?: emptyList()
        }
    }

    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
