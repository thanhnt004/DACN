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
        String url = baseUrl + "/v1beta/models/" + embeddingModel + ":embedContent";

        Map<String, Object> request = Map.of(
            "model", "models/" + embeddingModel,
            "content", Map.of(
                "parts", List.of(Map.of("text", text))
            )
        );

        log.debug("Generating embedding with model: {}, URL: {}", embeddingModel, url);
        
        return webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-goog-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseEmbeddingResponse)
                .doOnError(error -> log.error("Error generating embedding: {}", error.getMessage()));
    }

    public Mono<String> generateChatResponse(String prompt, String context) {
        String url = baseUrl + "/v1beta/models/" + chatModel + ":generateContent";

        log.debug("Generating chat response with model: {}, URL: {}", chatModel, url);

        String fullPrompt = String.format("""
            Bạn là một trợ lý AI cho cửa hàng thời trang WearWave. Nhiệm vụ của bạn là tư vấn và giúp khách hàng tìm kiếm sản phẩm.
            - Luôn trả lời một cách thân thiện, chuyên nghiệp và hữu ích.
            - Sử dụng thông tin sản phẩm được cung cấp dưới đây để trả lời câu hỏi của khách hàng một cách chính xác nhất.
            - Nếu không có thông tin sản phẩm nào được cung cấp hoặc thông tin không khớp với câu hỏi, đừng nói "tôi không biết". Thay vào đó, hãy:
                + Đưa ra lời khuyên thời trang chung liên quan đến câu hỏi.
                + Khuyến khích khách hàng khám phá các danh mục sản phẩm trên trang web.
                + Đề nghị họ cung cấp thêm chi tiết để bạn có thể hỗ trợ tốt hơn.
            - Giữ câu trả lời ngắn gọn và tập trung vào câu hỏi chính.

            Thông tin sản phẩm (nếu có):
            ---
            %s
            ---
            
            Câu hỏi của khách hàng: %s
            """, context, prompt);

        Map<String, Object> request = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", fullPrompt))
            ))
        );

        return webClient.post()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("x-goog-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseChatResponse)
                .doOnError(error -> log.error("Error generating chat response: {}", error.getMessage()));
    }

    private List<Double> parseEmbeddingResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embedding = jsonNode.path("embedding").path("values");

            if (embedding.isArray()) {
                return objectMapper.convertValue(embedding, List.class);
            }
            throw new RuntimeException("Invalid embedding response format");
        } catch (JsonProcessingException e) {
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
            return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này. Vui lòng thử lại sau.";
        }
    }
}

