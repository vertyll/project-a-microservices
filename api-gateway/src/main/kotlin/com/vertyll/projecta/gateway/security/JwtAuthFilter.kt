package com.vertyll.projecta.gateway.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Key

@Component
class JwtAuthFilter(
    @Value("\${security.jwt.secret-key}") private val secretKey: String
) : WebFilter {
    
    private val logger = LoggerFactory.getLogger(JwtAuthFilter::class.java)
    
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractTokenFromRequest(exchange.request)
        
        if (token == null) {
            return chain.filter(exchange)
        }
        
        return try {
            val claims = extractAllClaims(token)
            val username = claims.subject
            
            // Extract roles from token
            @Suppress("UNCHECKED_CAST")
            val roles = claims["roles"] as? List<String> ?: emptyList()
            
            // Create authorities from roles
            val authorities = roles.map { SimpleGrantedAuthority(it) }
            
            // Create authentication
            val authentication = UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
            )
            
            // Add authentication to security context
            chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        } catch (ex: JwtException) {
            logger.debug("Invalid JWT token: {}", ex.message)
            chain.filter(exchange)
        } catch (ex: Exception) {
            logger.error("Error processing JWT token", ex)
            chain.filter(exchange)
        }
    }
    
    private fun extractTokenFromRequest(request: ServerHttpRequest): String? {
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION) ?: return null
        
        if (!authHeader.startsWith("Bearer ")) {
            return null
        }
        
        return authHeader.substring(7)
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
