package com.example.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${google.ai.gemini.api-key}")
    private String apiKey;

    @Value("${google.ai.gemini.base-url}")
    private String baseUrl;

    @Value("${google.ai.gemini.embedding-model}")
    private String embeddingModel;

    @Value("${google.ai.gemini.chat-model}")
    private String chatModel;

    public Mono<List<Double>> generateEmbedding(String text) {
        // Xây dựng path động
        String path = "/v1beta/models/" + embeddingModel + ":embedContent";

        Map<String, Object> request = Map.of(
            "model", "models/" + embeddingModel,
            "content", Map.of(
                "parts", List.of(Map.of("text", text))
            )
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("generativelanguage.googleapis.com")
                        .path(path)
                        .queryParam("key", apiKey) // QUAN TRỌNG: Truyền Key qua tham số URL
                        .build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseEmbeddingResponse)
                .doOnError(error -> log.error("Error generating embedding: {}", error.getMessage()));
    }

    public Mono<String> generateChatResponse(String prompt, String context) {
        // 1. Tự ghép chuỗi URL hoàn chỉnh để kiểm soát dấu ":" và "?key="
        // Lưu ý: key phải là tham số cuối cùng
        String finalUrl = baseUrl + "/v1beta/models/" + chatModel + ":generateContent?key=" + apiKey;

        log.info("Chat URL (Raw): {}", finalUrl); // Log để kiểm tra

        String fullPrompt = String.format("""
            Bạn là một trợ lý AI cho cửa hàng thời trang WearWave. Nhiệm vụ của bạn là tư vấn và giúp khách hàng tìm kiếm sản phẩm.
            - Luôn trả lời một cách thân thiện, chuyên nghiệp và hữu ích.
            - Dựa vào thông tin sản phẩm dưới đây (nếu có) để trả lời.
            
            Thông tin sản phẩm:
            ---
            %s
            ---
            
            Câu hỏi: %s
            """, context, prompt);

        Map<String, Object> request = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", fullPrompt))
            ))
        );

        return webClient.post()
                // 2. Dùng URI.create() để Spring không can thiệp sửa URL của bạn nữa
                .uri(URI.create(finalUrl))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseChatResponse)
                .doOnError(error -> log.error("Error generating chat response: {}", error.getMessage()));
    }

    private List<Double> parseEmbeddingResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            // Kiểm tra cấu trúc response của Embedding
            JsonNode embedding = jsonNode.path("embedding").path("values");
            
            // Nếu model text-embedding-004 trả về cấu trúc khác, nó có thể nằm trong mảng embeddings
            if (embedding.isMissingNode()) {
                 embedding = jsonNode.path("embeddings").get(0).path("values");
            }

            if (embedding.isArray()) {
                return objectMapper.convertValue(embedding, List.class);
            }
            throw new RuntimeException("Invalid embedding response format: " + response);
        } catch (Exception e) {
            log.error("Error parsing embedding response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse embedding response", e);
        }
    }

    private String parseChatResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
        } catch (Exception e) {
            log.error("Error parsing chat response: {}", e.getMessage());
            return "Xin lỗi, hiện tại tôi đang gặp chút sự cố. Bạn vui lòng thử lại sau nhé!";
        }
    }
}