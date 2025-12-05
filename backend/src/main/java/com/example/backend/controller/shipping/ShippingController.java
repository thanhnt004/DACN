package com.example.backend.controller.shipping;

import com.example.backend.dto.response.shipping.ShipmentResponse;
import com.example.backend.service.facade.OrderFacadeService;
import com.example.backend.service.shipping.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipping/")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService service;
    private final OrderFacadeService orderFacadeService;
    @GetMapping("/{orderId}")
    public ResponseEntity<ShipmentResponse> getShipmentForOrder(@PathVariable UUID orderId)
    {
        return ResponseEntity.ok(service.getShipmentForOrder(orderId));
    }
    @PostMapping("/admin/orders/{orderId}/retry-return-shipment")
    public ResponseEntity<Void> retryCreateReturnShipment(@PathVariable UUID orderId) {
        orderFacadeService.retryCreateReturnShipment(orderId);
        return ResponseEntity.ok().build();
    }
}
