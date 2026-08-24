package com.vertyll.veds.file.infrastructure.storage

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "veds.file.storage")
data class ObjectStorageProperties(
    val endpoint: String,
    val region: String,
    val bucket: String,
    val accessKey: String,
    val secretKey: String,
    val uploadUrlValidity: Duration = Duration.ofMinutes(15),
    val downloadUrlValidity: Duration = Duration.ofMinutes(5),
)
