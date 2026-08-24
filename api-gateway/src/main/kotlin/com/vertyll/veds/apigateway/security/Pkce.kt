package com.vertyll.veds.apigateway.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal object Pkce {
    private const val VERIFIER_BYTES = 32
    private const val STATE_BYTES = 32
    private const val SHA_256 = "SHA-256"

    const val CHALLENGE_METHOD = "S256"

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    fun newCodeVerifier(): String = encoder.encodeToString(ByteArray(VERIFIER_BYTES).also(random::nextBytes))

    fun newState(): String = encoder.encodeToString(ByteArray(STATE_BYTES).also(random::nextBytes))

    fun challengeOf(codeVerifier: String): String {
        val digest = MessageDigest.getInstance(SHA_256).digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return encoder.encodeToString(digest)
    }
}
