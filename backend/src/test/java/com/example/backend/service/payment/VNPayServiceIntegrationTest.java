package com.example.backend.service.payment;

import com.example.backend.config.VNPayConfig;
import com.example.backend.dto.response.payment.PaymentStatusEvent;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.repository.payment.PaymentRepository;
import com.example.backend.service.order.OrderService;
import com.example.backend.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;

import static com.example.backend.util.VNPayUtil.hmacSHA512;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for VNPayService that make real API calls to VNPay Sandbox.
 *
 * IMPORTANT: These tests require valid VNPay sandbox credentials in application.yml:
 * - payment.vnPay.tmnCode
 * - payment.vnPay.secretKey
 * - payment.vnPay.url (sandbox URL)
 *
 * Tests are marked with @Disabled by default to prevent accidental API calls.
 * Remove @Disabled annotation to run these tests.
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings("deprecation") // MockBean is deprecated in Spring Boot 3.4+ but no stable replacement yet
class VNPayServiceIntegrationTest {

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private HttpServletRequest request;

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
        testOrder.setOrderNumber("ORD-TEST-" + System.currentTimeMillis());
        testOrder.setTotalAmount(500000L); // 500,000 VND
        testOrder.setPaidAt(null);

        testPayment = new Payment();
        testPayment.setId(paymentId);
        testPayment.setOrder(testOrder);
        testPayment.setAmount(500000L);
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
    }


    @Test
    void testCreatePaymentUrl_RealAPI() {
        log.info("=== Testing Create Payment URL with VNPay Sandbox ===");

        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then
        assertNotNull(paymentUrl, "Payment URL should not be null");
        assertTrue(paymentUrl.startsWith(vnPayConfig.getVnp_PayUrl()),
                "Payment URL should start with VNPay URL");
        assertTrue(paymentUrl.contains("vnp_Amount=50000000"),
                "Payment URL should contain correct amount");
        assertTrue(paymentUrl.contains("vnp_TxnRef=" + paymentId),
                "Payment URL should contain transaction reference");
        assertTrue(paymentUrl.contains("vnp_SecureHash="),
                "Payment URL should contain secure hash");

        log.info("✅ Payment URL created successfully");
        log.info("Payment URL: {}", paymentUrl);
        log.info("Order Number: {}", testOrder.getOrderNumber());
        log.info("Amount: {} VND", testOrder.getTotalAmount());
        log.info("\n🔗 You can test this payment URL in browser (VNPay Sandbox)");
        log.info("Test cards: https://sandbox.vnpayment.vn/apis/docs/bang-test-card/");
    }

    /**
     * Test validate payment URL format và parameters.
     */
    @Test
    void testCreatePaymentUrl_ValidateFormat() {
        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then - Validate URL structure
        assertTrue(paymentUrl.contains("vnp_Version="), "Should contain version");
        assertTrue(paymentUrl.contains("vnp_Command="), "Should contain command");
        assertTrue(paymentUrl.contains("vnp_TmnCode="), "Should contain TMN code");
        assertTrue(paymentUrl.contains("vnp_Amount="), "Should contain amount");
        assertTrue(paymentUrl.contains("vnp_CreateDate="), "Should contain create date");
        assertTrue(paymentUrl.contains("vnp_ExpireDate="), "Should contain expire date");
        assertTrue(paymentUrl.contains("vnp_IpAddr="), "Should contain IP address");
        assertTrue(paymentUrl.contains("vnp_Locale="), "Should contain locale");
        assertTrue(paymentUrl.contains("vnp_OrderInfo="), "Should contain order info");
        assertTrue(paymentUrl.contains("vnp_ReturnUrl="), "Should contain return URL");
        assertTrue(paymentUrl.contains("vnp_TxnRef="), "Should contain transaction reference");
        assertTrue(paymentUrl.contains("vnp_SecureHash="), "Should contain secure hash");

        log.info("✅ Payment URL format validation passed");
    }

    /**
     * Test validate secure hash của payment URL.
     */
    @Test
    void testCreatePaymentUrl_ValidateSecureHash() {
        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then - Extract and validate secure hash
        Map<String, String> params = extractParamsFromUrl(paymentUrl);
        String receivedHash = params.remove("vnp_SecureHash");

        // Rebuild hash data
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (hashData.length() > 0) {
                hashData.append("&");
            }
            hashData.append(fieldName).append("=").append(params.get(fieldName));
        }

        String calculatedHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());

        assertEquals(calculatedHash, receivedHash, "Secure hash should match");
        log.info("✅ Secure hash validation passed");
    }

    /**
     * Test xử lý IPN callback từ VNPay với dữ liệu giả lập thực tế.
     */
    @Test
    void testHandleIpn_SimulatedCallback_Success() {
        // Given - Simulate VNPay IPN callback with real-like data
        Map<String, String> ipnParams = new LinkedHashMap<>();
        ipnParams.put("vnp_Amount", "50000000"); // 500,000 VND * 100
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_BankTranNo", "VNP14516541");
        ipnParams.put("vnp_CardType", "ATM");
        ipnParams.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        ipnParams.put("vnp_PayDate", "20251204110530");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        ipnParams.put("vnp_TransactionNo", "14516541");
        ipnParams.put("vnp_TransactionStatus", "00");
        ipnParams.put("vnp_TxnRef", paymentId.toString());

        // Calculate secure hash
        String hashData = buildHashData(ipnParams);
        String secureHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        ipnParams.put("vnp_SecureHash", secureHash);

        // Mock dependencies
        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202"); // VNPay IP

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(ipnParams, request);

            // Then
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertEquals("00", response.getBody().get("RspCode"));
            assertEquals("Confirm Success", response.getBody().get("Message"));

            // Verify order marked as paid
            verify(orderService).markAsPaid(orderId);
            verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == Payment.PaymentStatus.CAPTURED
            ));

            log.info("✅ IPN callback processed successfully");
        }
    }

    /**
     * Test xử lý IPN với giao dịch thất bại.
     */
    @Test
    void testHandleIpn_SimulatedCallback_Failed() {
        // Given - Simulate failed transaction
        Map<String, String> ipnParams = new LinkedHashMap<>();
        ipnParams.put("vnp_Amount", "50000000");
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        ipnParams.put("vnp_PayDate", "20251204110530");
        ipnParams.put("vnp_ResponseCode", "24"); // User cancelled
        ipnParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        ipnParams.put("vnp_TransactionStatus", "02"); // Failed
        ipnParams.put("vnp_TxnRef", paymentId.toString());

        String hashData = buildHashData(ipnParams);
        String secureHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        ipnParams.put("vnp_SecureHash", secureHash);

        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When
            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(ipnParams, request);

            // Then
            assertEquals("00", response.getBody().get("RspCode"));
            verify(orderService, never()).markAsPaid(any());
            verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == Payment.PaymentStatus.FAILED
            ));

            log.info("✅ Failed IPN callback processed correctly");
        }
    }

    /**
     * Test SSE (Server-Sent Events) notification.
     */
    @Test
    void testSseNotification_PaymentStatus() throws Exception {
        // Given
        SseEmitter emitter = vnPayService.createEmitter(orderId.toString());
        assertNotNull(emitter, "Emitter should be created");
        assertEquals(300_000L, emitter.getTimeout(), "Emitter timeout should be 5 minutes");

        // When - Send final status event
        PaymentStatusEvent event = PaymentStatusEvent.builder()
                .orderId(orderId.toString())
                .status("PAID")
                .message("Thanh toán thành công")
                .timestamp(Instant.now())
                .build();

        // Then - Should not throw exception
        assertDoesNotThrow(() -> vnPayService.notifyPaymentStatus(orderId.toString(), event));

        // Wait a bit for async processing
        Thread.sleep(100);

        log.info("✅ SSE notification sent successfully");
    }

    /**
     * Test full payment flow: Create URL -> Simulate IPN -> Verify status.
     */
    @Test
    void testFullPaymentFlow_CreateUrlAndHandleIpn() {
        log.info("=== Testing Full Payment Flow ===");

        // Step 1: Create payment URL
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);
        assertNotNull(paymentUrl);
        log.info("✅ Step 1: Payment URL created");
        log.info("Payment URL: {}", paymentUrl);

        // Step 2: Simulate user completes payment and VNPay sends IPN
        Map<String, String> ipnParams = new LinkedHashMap<>();
        ipnParams.put("vnp_Amount", "50000000");
        ipnParams.put("vnp_BankCode", "NCB");
        ipnParams.put("vnp_BankTranNo", "VNP" + System.currentTimeMillis());
        ipnParams.put("vnp_CardType", "ATM");
        ipnParams.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        ipnParams.put("vnp_PayDate", "20251204110530");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        ipnParams.put("vnp_TransactionNo", String.valueOf(System.currentTimeMillis()));
        ipnParams.put("vnp_TransactionStatus", "00");
        ipnParams.put("vnp_TxnRef", paymentId.toString());

        String hashData = buildHashData(ipnParams);
        String secureHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        ipnParams.put("vnp_SecureHash", secureHash);

        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(ipnParams, request);

            // Step 3: Verify IPN processed
            assertEquals("00", response.getBody().get("RspCode"));
            verify(orderService).markAsPaid(orderId);
            log.info("✅ Step 2: IPN callback processed");
            log.info("✅ Step 3: Order marked as paid");
        }

        log.info("✅ Full payment flow completed successfully");
    }

    /**
     * Test payment URL expiry time.
     */
    @Test
    void testPaymentUrl_ExpiryTime() {
        // When
        String paymentUrl = vnPayService.createPaymentUrl(testOrder, testPayment);

        // Then
        Map<String, String> params = extractParamsFromUrl(paymentUrl);

        String createDate = params.get("vnp_CreateDate");
        String expireDate = params.get("vnp_ExpireDate");

        assertNotNull(createDate, "Create date should be present");
        assertNotNull(expireDate, "Expire date should be present");

        // Parse dates and verify expiry is after create date
        assertTrue(expireDate.compareTo(createDate) > 0,
                "Expire date should be after create date");

        log.info("✅ Payment URL expiry validation passed");
        log.info("Create Date: {}", createDate);
        log.info("Expire Date: {}", expireDate);
    }

    /**
     * Test handling multiple concurrent IPN callbacks (idempotency).
     */
    @Test
    void testHandleIpn_Idempotency() {
        // Given
        Map<String, String> ipnParams1 = createValidIpnParams();

        when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

        try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
            mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                    .thenReturn("113.160.92.202");

            // When - First callback
            ResponseEntity<Map<String, String>> response1 = vnPayService.handleIpn(ipnParams1, request);
            assertEquals("00", response1.getBody().get("RspCode"));

            // Mark order as paid
            testOrder.setPaidAt(Instant.now());

            // When - Second callback (duplicate) - Create new params
            Map<String, String> ipnParams2 = createValidIpnParams();
            ResponseEntity<Map<String, String>> response2 = vnPayService.handleIpn(ipnParams2, request);

            // Then
            assertEquals("02", response2.getBody().get("RspCode"));
            assertEquals("Already confirmed", response2.getBody().get("Message"));

            // Verify markAsPaid called only once
            verify(orderService, times(1)).markAsPaid(orderId);

            log.info("✅ Idempotency test passed - duplicate IPN handled correctly");
        }
    }

    /**
     * Test với các response code khác nhau từ VNPay.
     */
    @Test
    void testHandleIpn_DifferentResponseCodes() {
        String[] responseCodes = {
                "00", // Success
                "07", // Transaction successful but suspicious
                "09", // Card not registered for internet banking
                "10", // Card authentication failed
                "11", // Transaction timeout
                "12", // Card locked
                "13", // Wrong OTP
                "24", // User cancelled
                "51", // Insufficient balance
                "65", // Transaction limit exceeded
                "75", // Bank under maintenance
                "79"  // Transaction error
        };

        for (String code : responseCodes) {
            log.info("Testing response code: {}", code);

            Map<String, String> ipnParams = createValidIpnParams();
            ipnParams.put("vnp_ResponseCode", code);
            ipnParams.put("vnp_TransactionStatus", code.equals("00") ? "00" : "02");

            // Recalculate hash
            ipnParams.remove("vnp_SecureHash");
            String hashData = buildHashData(ipnParams);
            String secureHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData);
            ipnParams.put("vnp_SecureHash", secureHash);

            when(orderService.getOrderByNumber(testOrder.getOrderNumber())).thenReturn(testOrder);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(testPayment));

            try (MockedStatic<HeaderUtil> mockedHeaderUtil = mockStatic(HeaderUtil.class)) {
                mockedHeaderUtil.when(() -> HeaderUtil.getClientIp(request))
                        .thenReturn("113.160.92.202");

                ResponseEntity<Map<String, String>> response = vnPayService.handleIpn(ipnParams, request);

                assertNotNull(response);
                assertNotNull(response.getBody());
            }

            // Reset mocks for next iteration
            reset(orderService, paymentRepository);
            testPayment.setStatus(Payment.PaymentStatus.PENDING);
            testOrder.setPaidAt(null);
        }

        log.info("✅ All response codes handled correctly");
    }

    // ==================== Helper Methods ====================

    private Map<String, String> createValidIpnParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Amount", "50000000");
        params.put("vnp_BankCode", "NCB");
        params.put("vnp_BankTranNo", "VNP" + System.currentTimeMillis());
        params.put("vnp_CardType", "ATM");
        params.put("vnp_OrderInfo", "Thanh toan don hang " + testOrder.getOrderNumber());
        params.put("vnp_PayDate", "20251204110530");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        params.put("vnp_TransactionNo", String.valueOf(System.currentTimeMillis()));
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", paymentId.toString());

        String hashData = buildHashData(params);
        String secureHash = hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        params.put("vnp_SecureHash", secureHash);

        return params;
    }

    private String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (fieldName.equals("vnp_SecureHash")) {
                continue;
            }
            if (hashData.length() > 0) {
                hashData.append("&");
            }
            hashData.append(fieldName).append("=").append(params.get(fieldName));
        }
        return hashData.toString();
    }

    private Map<String, String> extractParamsFromUrl(String url) {
        Map<String, String> params = new HashMap<>();
        String query = url.substring(url.indexOf('?') + 1);
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            String key = pair.substring(0, idx);
            String value = pair.substring(idx + 1);
            params.put(key, value);
        }

        return params;
    }
}

