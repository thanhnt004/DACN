package com.example.backend.controller.webhook;

import com.example.backend.dto.ghn.GhnWebhookRequest;
import com.example.backend.service.shipping.ShipmentSyncService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);
    private final ShipmentSyncService shipmentSyncService;

    @PostMapping("/ghn")
    @Hidden // Ẩn khỏi Swagger public
    public ResponseEntity<String> handleGhnWebhook(@RequestBody GhnWebhookRequest request) {
        log.info("Received GHN Webhook: Code={}, Status={}", request.orderCode(), request.status());

        try {
            shipmentSyncService.updateShipmentStatus(request);
        } catch (Exception e) {
            log.error("Error processing GHN webhook", e);
        }
        return ResponseEntity.ok("ACK");
    }
}