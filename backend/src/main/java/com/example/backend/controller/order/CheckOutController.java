package com.example.backend.controller.order;

import com.example.backend.aop.Idempotent;
import com.example.backend.dto.request.checkout.CheckoutSessionCreateRequest;
import com.example.backend.dto.request.checkout.UpdateAddressRequest;
import com.example.backend.dto.request.checkout.UpdateDiscountRequest;
import com.example.backend.dto.request.checkout.UpdatePaymentMethodRequest;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.dto.response.checkout.OrderCreatedResponse;
import com.example.backend.service.facade.CheckoutFacadeService;
import com.example.backend.service.facade.OrderFacadeService;
import com.example.backend.validate.RequireSessionOwnership;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/checkout")
@RequiredArgsConstructor
public class CheckOutController
{
    private final CheckoutFacadeService checkoutFacade;
    private final OrderFacadeService orderFacade;
    /**
     * Tạo checkout session
     *
     * @param request Chứa items, source, discount
     * @return CheckoutSessionResponse với sessionId + sessionToken
     */
    @PostMapping("/sessions")
    public ResponseEntity<CheckoutSession> createSession(
            @Valid @RequestBody CheckoutSessionCreateRequest request
    ) {

        CheckoutSession response = checkoutFacade.createCheckoutSession(
                request
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Lấy thông tin session
     */
    @GetMapping("/sessions/{sessionId}")
    @RequireSessionOwnership
    public ResponseEntity<CheckoutSession> getSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Session-Token") String sessionToken
    ) {
        CheckoutSession response = checkoutFacade.getSession(
                sessionId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật địa chỉ giao hàng
     */
    @PutMapping("/sessions/{sessionId}/address")
    @RequireSessionOwnership
    public ResponseEntity<CheckoutSession> updateAddress(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody UpdateAddressRequest request
    ) {

        CheckoutSession response = checkoutFacade.updateAddress(
                sessionId,
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật mã giảm giá
     */
    @PutMapping("/sessions/{sessionId}/discount")
    @RequireSessionOwnership
    public ResponseEntity<CheckoutSession> updateDiscount(
            @PathVariable String sessionId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody UpdateDiscountRequest request
    ) {

        CheckoutSession response = checkoutFacade.updateDiscount(
                sessionId,
                request
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật phương thức thanh toán
     */
    @PutMapping("/sessions/{sessionId}/payment-method")
    @RequireSessionOwnership
    public ResponseEntity<CheckoutSession> updatePaymentMethod(
            @PathVariable String sessionId,
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody UpdatePaymentMethodRequest request
    ) {

        CheckoutSession response = checkoutFacade.updatePaymentMethod(
                sessionId,
                request
        );

        return ResponseEntity.ok(response);
    }
    /**
     * Confirm checkout và tạo đơn hàng
     */
    @Idempotent(expire = 300, scope = "sessionId")
    @PostMapping("/sessions/{sessionId}/confirm")
    @RequireSessionOwnership
    public ResponseEntity<OrderCreatedResponse> confirmCheckout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            @PathVariable("sessionId") UUID sessionId,
            @RequestBody(required = false) java.util.Map<String, String> payload,
            @RequestHeader("X-Session-Token") String sessionToken
    ) {
        String notes = payload != null ? payload.getOrDefault("notes", "") : "";
        OrderCreatedResponse order = orderFacade.confirmCheckoutSession(sessionId,httpRequest,httpResponse,notes);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(order);
    }
}
