package com.vertyll.projecta.common.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(SharedConfigProperties::class)
class SharedConfigAutoConfiguration