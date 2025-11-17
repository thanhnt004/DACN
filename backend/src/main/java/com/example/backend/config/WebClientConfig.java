package com.example.backend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    /**
     * WebClient chung cho các dịch vụ bên thứ ba
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Cấu hình connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // Cấu hình HTTP client với timeout
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)))
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .filter(errorHandler());
    }

    /**
     * Log request (không log sensitive data)
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                // Không log headers có chứa token/secret
                clientRequest.headers().forEach((name, values) -> {
                    if (!name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                        log.debug("{}: {}", name, values);
                    }
                });
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Log response
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("Response status: {}", clientResponse.statusCode());
            }
            return Mono.just(clientResponse);
        });
    }

    /**
     * Xử lý lỗi chung
     */
    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("External API error: {} - {}",
                                    clientResponse.statusCode(), errorBody);
                            return Mono.just(clientResponse);
                        });
            }
            return Mono.just(clientResponse);
        });
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WebClientConfig.class);
}