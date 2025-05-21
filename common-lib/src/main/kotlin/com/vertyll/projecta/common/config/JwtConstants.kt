package com.vertyll.projecta.common.config

object JwtConstants {
    // JWT field names and claim keys
    const val CLAIM_ROLES = "roles"
    const val CLAIM_TOKEN_ID = "tokenId"
    const val CLAIM_TYPE = "type"

    // Token types
    const val TOKEN_TYPE_ACCESS = "access"
    const val TOKEN_TYPE_REFRESH = "refresh"

    // JWT default/fallback values
    const val DEFAULT_ACCESS_TOKEN_EXPIRATION = 900000L  // 15 minutes in milliseconds
    const val DEFAULT_REFRESH_TOKEN_EXPIRATION = 604800000L  // 7 days in milliseconds
    const val DEFAULT_REFRESH_TOKEN_COOKIE_NAME = "refresh_token"

    // Default JWT secret key - should be overridden in production
    const val DEFAULT_SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"

    // Header constants
    const val BEARER_PREFIX = "Bearer "
}
