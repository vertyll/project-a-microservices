package com.vertyll.veds.iam.application.dto

data class SecuritySettingsResponse(
    val twoFactorEnabled: Boolean,
    val configuredFactors: List<String>,
)
