package com.vertyll.projecta.gateway.config

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
class GatewayConfig {
    @Bean
    fun customGlobalFilter(): GlobalFilter {
        return HeadersExchangeFilter()
    }

    private class HeadersExchangeFilter : GlobalFilter, Ordered {
        private val log = LoggerFactory.getLogger(HeadersExchangeFilter::class.java)

        override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

        override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
            val request = exchange.request
            val authorization = request.headers.getFirst(HttpHeaders.AUTHORIZATION)

            if (authorization != null) {
                log.debug("Propagating Authorization header to downstream service: {}", request.path)

                /*
                val mutatedRequest = exchange.request.mutate()
                    .header("X-Custom-Header", "custom-value")
                    .build()

                return chain.filter(exchange.mutate().request(mutatedRequest).build())
                 */
            } else {
                log.debug("No Authorization header found for path: {}", request.path)
            }

            return chain.filter(exchange)
        }
    }
}
