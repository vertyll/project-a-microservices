package com.vertyll.projecta.gateway.config

import com.vertyll.projecta.common.config.SharedConfigProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SharedConfigProperties::class)
class SharedConfigImport
