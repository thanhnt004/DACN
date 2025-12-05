package com.example.backend.controller.order;

import com.example.backend.dto.request.order.PaymentRefundOption;
import com.example.backend.dto.response.checkout.OrderChangeRequestResponse;
import com.example.backend.service.facade.OrderFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping(value = "/api/v1/admin/orders/change-request")
@RestController
@RequiredArgsConstructor
public class OrderChangeRequestController {
    private final OrderFacadeService orderFacadeService;
    @GetMapping(value = "/{orderId}")
    public ResponseEntity<OrderChangeRequestResponse> getChangeRequest(@PathVariable UUID orderId)
    {
       return ResponseEntity.ok(orderFacadeService.getChangeRequestForOrder(orderId));
    }
    @PutMapping(value = "/{id}")
    public ResponseEntity<?> changeRefundInfo(@PathVariable("id") UUID changRequestId,
                                              @RequestBody PaymentRefundOption refundOption,
                                              HttpServletResponse response,
                                              HttpServletRequest request
                                              ){
        orderFacadeService.updateRefundPaymentInfo(changRequestId,refundOption,request,response);
        return ResponseEntity.noContent().build();
    }
}
