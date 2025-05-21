package com.vertyll.projecta.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "projecta.shared")
class SharedConfigProperties(
    val security: SecurityProperties = SecurityProperties(),
    val services: ServicesProperties = ServicesProperties()
) {
    class SecurityProperties(
        val jwt: JwtProperties = JwtProperties()
    ) {
        class JwtProperties(
            val secretKey: String = JwtConstants.DEFAULT_SECRET_KEY,
            val accessTokenExpiration: Long = JwtConstants.DEFAULT_ACCESS_TOKEN_EXPIRATION,
            val refreshTokenExpiration: Long = JwtConstants.DEFAULT_REFRESH_TOKEN_EXPIRATION,
            val refreshTokenCookieName: String = JwtConstants.DEFAULT_REFRESH_TOKEN_COOKIE_NAME
        )
    }

    class ServicesProperties(
        val authService: ServiceProperties = ServiceProperties("http://localhost:8082"),
        val userService: ServiceProperties = ServiceProperties("http://localhost:8083"),
        val roleService: ServiceProperties = ServiceProperties("http://localhost:8084"),
        val mailService: ServiceProperties = ServiceProperties("http://localhost:8085")
    ) {
        class ServiceProperties(
            val url: String
        )
    }
}
