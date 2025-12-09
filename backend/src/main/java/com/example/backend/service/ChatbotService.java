package com.example.backend.service;

import com.example.backend.entity.ProductEmbedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotService {

    private final ProductEmbeddingService embeddingService;
    private final GeminiService geminiService;

    public Mono<String> processChatQuery(String userQuery) {
        log.info("Processing chat query: {}", userQuery);

        try {
            List<ProductEmbedding> similarProducts = embeddingService.searchSimilarProducts(userQuery, 5);

            String context = similarProducts.stream()
                    .map(ProductEmbedding::getContent)
                    .collect(Collectors.joining("\n---\n"));

            return geminiService.generateChatResponse(userQuery, context);

        } catch (Exception e) {
            log.error("Error processing chat query: {}", e.getMessage(), e);
            // Fallback to Gemini with no context if embedding search fails
            return geminiService.generateChatResponse(userQuery, "");
        }
    }
}

