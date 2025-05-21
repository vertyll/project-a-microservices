package com.vertyll.projecta.auth.domain.service

import com.vertyll.projecta.common.config.JwtConstants
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.time.Instant
import java.util.Date
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class JwtService {
    
    @Value("\${security.jwt.secret-key}")
    private lateinit var secretKey: String

    @Value("\${security.jwt.access-token-expiration:${JwtConstants.DEFAULT_ACCESS_TOKEN_EXPIRATION}}")
    private var accessTokenExpiration: Long = JwtConstants.DEFAULT_ACCESS_TOKEN_EXPIRATION

    @Value("\${security.jwt.refresh-token-expiration:${JwtConstants.DEFAULT_REFRESH_TOKEN_EXPIRATION}}")
    private var refreshTokenExpiration: Long = JwtConstants.DEFAULT_REFRESH_TOKEN_EXPIRATION

    @Value("\${security.jwt.refresh-token-cookie-name:${JwtConstants.DEFAULT_REFRESH_TOKEN_COOKIE_NAME}}")
    private var refreshTokenCookieName: String = JwtConstants.DEFAULT_REFRESH_TOKEN_COOKIE_NAME

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
            .setClaims(extraClaims)
            .setSubject(userDetails.username)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(accessTokenExpiration)))
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
            .setClaims(extraClaims)
            .setSubject(userDetails.username)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusMillis(refreshTokenExpiration)))
            .setId(UUID.randomUUID().toString()) // Add a unique ID to each token
            .signWith(getSigningKey())
            .compact()
    }

    fun getRefreshTokenCookieName(): String {
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
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun getSigningKey(): Key {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        return Keys.hmacShaKeyFor(keyBytes)
    }
}
