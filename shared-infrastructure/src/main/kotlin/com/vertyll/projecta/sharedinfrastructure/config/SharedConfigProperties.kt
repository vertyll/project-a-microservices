package com.vertyll.projecta.sharedinfrastructure.config

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
            val secretKey: String = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            val accessTokenExpiration: Long = 900000L,
            val refreshTokenExpiration: Long = 604800000L,
            val refreshTokenCookieName: String = "refresh_token",
            val authHeaderName: String = "Authorization"
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
