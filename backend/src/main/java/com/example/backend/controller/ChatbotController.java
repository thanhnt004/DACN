package com.example.backend.controller;

import com.example.backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    public Mono<ResponseEntity<Map<String, String>>> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.message());

        return chatbotService.processChatQuery(request.message())
                .map(response -> ResponseEntity.ok(Map.of(
                        "response", response,
                        "status", "success"
                )))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of(
                        "response", "Đã có lỗi xảy ra, vui lòng thử lại sau.",
                        "status", "error"
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy"));
    }

    public record ChatRequest(@NotBlank String message) {}
}

