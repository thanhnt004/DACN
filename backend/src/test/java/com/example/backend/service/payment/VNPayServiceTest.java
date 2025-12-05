package com.example.backend.service.payment;

import com.example.backend.config.VNPayConfig;
import com.example.backend.dto.response.payment.PaymentStatusEvent;
import com.example.backend.exception.order.OrderNotFoundException;
import com.example.backend.exception.payment.PaymentFailedException;
import com.example.backend.exception.payment.PaymentNotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.payment.PaymentRepository;
import com.example.backend.service.order.OrderService;
import com.example.backend.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;

import static com.example.backend.util.VNPayUtil.hmacSHA512;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VNPayService vnPayService;

    private Order testOrder;
    private Payment testPayment;
    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(orderId);
        testOrder.setOrderNumber("ORD-20231204-001");
        testOrder.setTotalAmount(500000L);
        testOrder.setPaidAt(null); // Not paid yet

        testPayment = new Payment();
        testPayment.setId(paymentId);
        testPayment.setOrder(testOrder);
        testPayment.setAmount(500000L);
        testPayment.setStatus(Payment.PaymentStatus.PENDING);

        // Set maxPaymentTime
        ReflectionTestUtils.setField(vnPayService, "maxPaymentTime", 15);
    }

    @Test
    void testCreatePaymentUrl_Success() {
        // Given
        Map<String, String> config = new HashMap<>();
        config.put("vnp_Version", "2.1.0");
        config.put("vnp_Command", "pay");
        config.put("vnp_TmnCode", "TEST_TMN");
        config.put("vnp_CurrCode", "VND");
        config.put("vnp_OrderType", "other");
        config.put("vnp_Locale", "vn");
        config.put("vnp_ReturnUrl", "http://localhost:8080/api/payment/vnpay/return");

        when(vnPayConfig.getConfig()).thenReturn(config);
        when(vnPayConfig.getVnp_PayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getSecretKey()).thenReturn("TESTKEY123456789");

        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then
        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"));
        assertTrue(paymentUrl.contains("vnp_Amount=50000000")); // 500000 * 100
        assertTrue(paymentUrl.contains("vnp_TxnRef=" + paymentId));
        assertTrue(paymentUrl.contains("vnp_OrderInfo=Thanh+toan+don+hang+ORD-20231204-001"));
        assertTrue(paymentUrl.contains("vnp_SecureHash="));

        log.info("Generated payment URL: {}", paymentUrl);
    }

    @Test
    void testCreatePaymentUrl_WithLargeAmount() {
        // Given
        testOrder.setTotalAmount(99999999L);
        Map<String, String> config = new HashMap<>();
        config.put("vnp_Version", "2.1.0");
        config.put("vnp_Command", "pay");
        config.put("vnp_TmnCode", "TEST_TMN");
        config.put("vnp_CurrCode", "VND");
        config.put("vnp_OrderType", "other");
        config.put("vnp_Locale", "vn");
        config.put("vnp_ReturnUrl", "http://localhost:8080/api/payment/vnpay/return");

        when(vnPayConfig.getConfig()).thenReturn(config);
        when(vnPayConfig.getVnp_PayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getSecretKey()).thenReturn("TESTKEY123456789");

        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then
        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.contains("vnp_Amount=9999999900")); // 99999999 * 100
    }

    @Test
    void testCreatePaymentUrl_ThrowsException() {
        // Given
        when(vnPayConfig.getConfig()).thenThrow(new RuntimeException("Config error"));

        // When & Then
        assertThrows(PaymentFailedException.class, () -> {
            vnPayService.createPaymentUrl(testOrder, testPayment);
        });
    }

    @Test
    void testHandleIpn_Success() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_Amount", "50000000");

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then
            assertEquals(200, response.getStatusCode().value());
            assertEquals("00", response.getBody().get("RspCode"));
            assertEquals("Confirm Success", response.getBody().get("Message"));

            verify(orderService).markAsPaid(orderId);
            verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == Payment.PaymentStatus.CAPTURED
            ));
        }
    }

    @Test
    void testHandleIpn_InvalidSignature() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_Amount", "50000000");
        params.put("vnp_SecureHash", "INVALID_HASH");

        when(vnPayConfig.getSecretKey()).thenReturn("TESTKEY123456789");

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then
            assertEquals("97", response.getBody().get("RspCode"));
            assertEquals("Invalid Signature", response.getBody().get("Message"));

            verify(orderService, never()).markAsPaid(any());
        }
    }

    @Test
    void testHandleIpn_OrderNotFound() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang INVALID-ORDER");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber("INVALID-ORDER")).thenThrow(new OrderNotFoundException("Order not found"));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then
            assertEquals("01", response.getBody().get("RspCode"));
            assertEquals("Order not found", response.getBody().get("Message"));
        }
    }

    @Test
    void testHandleIpn_AlreadyConfirmed() {
        // Given
        testOrder.setPaidAt(Instant.now()); // Mark as already paid

        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then
            assertEquals("02", response.getBody().get("RspCode"));
            assertEquals("Already confirmed", response.getBody().get("Message"));

            verify(orderService, never()).markAsPaid(any());
        }
    }

    @Test
    void testHandleIpn_TransactionFailed() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "01"); // Failed
        params.put("vnp_ResponseCode", "24"); // Cancelled by user

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then
            assertEquals("00", response.getBody().get("RspCode"));

            verify(orderService, never()).markAsPaid(any());
            verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == Payment.PaymentStatus.FAILED
            ));
        }
    }

    @Test
    void testHandleIpn_PaymentNotFound() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When & Then
            assertThrows(PaymentNotFoundException.class, () -> {
                vnPayService.handleIpn(params, request);
            });
        }
    }

    @Test
    void testHandleIpn_UnknownIP() {
        // Given
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", paymentId.toString());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_ResponseCode", "00");

        String secretKey = "TESTKEY123456789";
        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(secretKey, hashData);
        params.put("vnp_SecureHash", secureHash);

        when(vnPayConfig.getSecretKey()).thenReturn(secretKey);
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("192.168.1.1"); // Unknown IP

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(params, request);

            // Then - Still processes but logs warning
            assertEquals("00", response.getBody().get("RspCode"));
            verify(orderService).markAsPaid(orderId);
        }
    }

    @Test
    void testCreateEmitter_Success() {
        // When
        SseEmitter emitter = vnPayService.createEmitter(orderId.toString());

        // Then
        assertNotNull(emitter);
        assertEquals(300_000L, emitter.getTimeout());
    }

    @Test
    void testNotifyPaymentStatus_Success() throws Exception {
        // Given
        SseEmitter emitter = vnPayService.createEmitter(orderId.toString());
        PaymentStatusEvent event = PaymentStatusEvent.builder()
                .orderId(orderId.toString())
                .status("PAID")
                .message("Thanh toán thành công")
                .timestamp(Instant.now())
                .build();

        // When
        vnPayService.notifyPaymentStatus(orderId.toString(), event);

        // Then - No exception should be thrown
        // Emitter should be removed after final event
        Thread.sleep(100); // Give time for async processing
    }

    @Test
    void testNotifyPaymentStatus_NoActiveConnection() {
        // Given
        PaymentStatusEvent event = PaymentStatusEvent.builder()
                .orderId(orderId.toString())
                .status("PAID")
                .message("Thanh toán thành công")
                .timestamp(Instant.now())
                .build();

        // When - No emitter created
        vnPayService.notifyPaymentStatus(orderId.toString(), event);

        // Then - No exception should be thrown
        // Just logs debug message
    }

    @Test
    void testNotifyPaymentStatus_NonFinalStatus() throws Exception {
        // Given
        SseEmitter emitter = vnPayService.createEmitter(orderId.toString());
        PaymentStatusEvent event = PaymentStatusEvent.builder()
                .orderId(orderId.toString())
                .status("PROCESSING") // Non-final status
                .message("Đang xử lý thanh toán")
                .timestamp(Instant.now())
                .build();

        // When
        vnPayService.notifyPaymentStatus(orderId.toString(), event);

        // Then - Emitter should NOT be closed
        Thread.sleep(100);
    }

    @Test
    void testCreatePaymentUrl_WithSpecialCharacters() {
        // Given
        testOrder.setOrderNumber("ORD-2023/12/04-001");
        Map<String, String> config = new HashMap<>();
        config.put("vnp_Version", "2.1.0");
        config.put("vnp_Command", "pay");
        config.put("vnp_TmnCode", "TEST_TMN");
        config.put("vnp_CurrCode", "VND");
        config.put("vnp_OrderType", "other");
        config.put("vnp_Locale", "vn");
        config.put("vnp_ReturnUrl", "http://localhost:8080/api/payment/vnpay/return");

        when(vnPayConfig.getConfig()).thenReturn(config);
        when(vnPayConfig.getVnp_PayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        when(vnPayConfig.getSecretKey()).thenReturn("TESTKEY123456789");

        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then
        assertNotNull(paymentUrl);
        // Special characters should be URL encoded
        assertTrue(paymentUrl.contains("ORD-2023%2F12%2F04-001"));
    }

    // Helper method to build hash data
    private String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (hashData.length() > 0) {
                hashData.append("&");
            }
            hashData.append(fieldName).append("=").append(params.get(fieldName));
        }
        return hashData.toString();
    }
}

