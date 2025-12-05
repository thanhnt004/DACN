package com.example.backend.service.shipping;

import com.example.backend.dto.ghn.*;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.exception.shipping.ShippingServiceException;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.OrderItem;
import com.example.backend.model.order.Shipment;
import com.example.backend.model.order.ShipmentItem;
import com.example.backend.model.product.ProductVariant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class GHNServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private GHNService ghnService;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebClient.Builder testWebClientBuilder() {
            return WebClient.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024));
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ghn.api.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("ghn.api.token", () -> "test-token");
        registry.add("ghn.api.shop-id", () -> "123456");
        registry.add("ghn.defaults.timeout", () -> "5s");
        registry.add("ghn.api.urls.services", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/shipping-order/available-services");
        registry.add("ghn.api.urls.fee", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/shipping-order/fee");
        registry.add("ghn.api.urls.leadtime", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/shipping-order/leadtime");
        registry.add("ghn.api.urls.provinces", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/master-data/province");
        registry.add("ghn.api.urls.districts", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/master-data/district");
        registry.add("ghn.api.urls.wards", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/master-data/ward");
        registry.add("ghn.api.urls.shop-info", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/shop/all");
        registry.add("ghn.api.urls.create-order", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/shipping-order/create");
        registry.add("ghn.api.urls.print-order", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/a5/gen-token?token=");
        registry.add("ghn.api.urls.print-token", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/a5/gen-token");
        registry.add("ghn.api.urls.cancel-order", () -> "http://localhost:" + wireMockServer.port() + "/shiip/public-api/v2/switch-status/cancel");
    }

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUpEach() {
        wireMockServer.resetAll();
        setupCommonStubs();
    }

    private void setupCommonStubs() {
        // Mock shop info
        stubFor(get(urlEqualTo("/shiip/public-api/v2/shop/all"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "shops": [{
                                            "_id": 123456,
                                            "name": "Test Shop",
                                            "phone": "0123456789",
                                            "address": "123 Test Street",
                                            "ward_code": "20308",
                                            "district_id": 1542
                                        }]
                                    }
                                }
                                """)));

        // Mock provinces
        stubFor(get(urlEqualTo("/shiip/public-api/master-data/province"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "data": [{
                                        "ProvinceID": 202,
                                        "ProvinceName": "Hồ Chí Minh",
                                        "NameExtension": ["TP HCM", "TPHCM", "Ho Chi Minh City"]
                                    }]
                                }
                                """)));

        // Mock districts
        stubFor(post(urlEqualTo("/shiip/public-api/master-data/district"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "data": [{
                                        "DistrictID": 1542,
                                        "ProvinceID": 202,
                                        "DistrictName": "Quận 1",
                                        "NameExtension": ["Q1", "District 1"]
                                    }]
                                }
                                """)));

        // Mock wards
        stubFor(post(urlEqualTo("/shiip/public-api/master-data/ward"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "data": [{
                                        "WardCode": "20308",
                                        "DistrictID": 1542,
                                        "WardName": "Phường Bến Nghé",
                                        "NameExtension": ["Ben Nghe Ward"],
                                        "Status": 1
                                    }]
                                }
                                """)));
    }

    @Test
    @DisplayName("Tạo đơn hàng shipping thành công")
    void testCreateShippingOrder_Success() throws Exception {
        // Arrange
        String expectedOrderCode = "GHNTESTING";

        stubFor(post(urlEqualTo("/shiip/public-api/v2/shipping-order/create"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "order_code": "%s",
                                        "sort_code": "123-456-789",
                                        "trans_type": "fly",
                                        "ward_encode": "",
                                        "district_encode": "",
                                        "fee": {
                                            "main_service": 25000,
                                            "insurance": 0,
                                            "station_do": 0,
                                            "station_pu": 0,
                                            "return": 0,
                                            "r2s": 0,
                                            "return_again": 0,
                                            "coupon": 0,
                                            "document_return": 0,
                                            "double_check": 0,
                                            "double_check_deliver": 0,
                                            "pick_remote_areas_fee": 0,
                                            "deliver_remote_areas_fee": 0,
                                            "pick_remote_areas_fee_return": 0,
                                            "deliver_remote_areas_fee_return": 0,
                                            "cod_fee": 0,
                                            "cod_failed_fee": 0
                                        },
                                        "total_fee": 25000,
                                        "expected_delivery_time": "2024-12-05T00:00:00Z"
                                    }
                                }
                                """.formatted(expectedOrderCode))));

        CreateShippingOrder request = createTestShippingOrderRequest();

        // Act
        CreateOrderData result = ghnService.createShippingOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedOrderCode, result.getOrderCode());
        assertNotNull(result.getTotalFee());
        assertEquals(25000, result.getTotalFee());
    }

    @Test
    @DisplayName("Tạo đơn hàng shipping - API trả về lỗi")
    void testCreateShippingOrder_ApiError() {
        // Arrange
        stubFor(post(urlEqualTo("/shiip/public-api/v2/shipping-order/create"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 400,
                                    "message": "Invalid request data"
                                }
                                """)));

        CreateShippingOrder request = createTestShippingOrderRequest();

        // Act & Assert
        ShippingServiceException exception = assertThrows(
                ShippingServiceException.class,
                () -> ghnService.createShippingOrder(request)
        );

        assertEquals("SHIPPING_API_ERROR", exception.getCode());
        assertEquals("Không thể tạo đơn vận chuyển", exception.getMessage());
    }

    @Test
    @DisplayName("Lấy print token thành công")
    void testGetPrintToken_Success() {
        // Arrange
        String expectedToken = "test-print-token-12345";
        List<String> orderCodes = List.of("GHNTESTING", "GHNTESTING2");

        stubFor(post(urlEqualTo("/shiip/public-api/v2/a5/gen-token"))
                .withHeader("Token", equalTo("test-token"))
                .withRequestBody(containing("GHNTESTING"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "token": "%s"
                                    }
                                }
                                """.formatted(expectedToken))));

        // Act
        String result = ghnService.getPrintToken(orderCodes);

        // Assert
        assertNotNull(result);
        assertEquals(expectedToken, result);
    }

    @Test
    @DisplayName("Lấy print token - API trả về lỗi")
    void testGetPrintToken_ApiError() {
        // Arrange
        List<String> orderCodes = List.of("INVALID_ORDER");

        stubFor(post(urlEqualTo("/shiip/public-api/v2/a5/gen-token"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 400,
                                    "message": "Order not found"
                                }
                                """)));

        // Act & Assert
        ShippingServiceException exception = assertThrows(
                ShippingServiceException.class,
                () -> ghnService.getPrintToken(orderCodes)
        );

        assertEquals("SHIPPING_API_ERROR", exception.getCode());
        assertEquals("Không thể tạo print token", exception.getMessage());
    }

    @Test
    @DisplayName("Lấy URL in đơn hàng thành công")
    void testGetPrintOrderUrl_Success() {
        // Arrange
        String expectedToken = "test-print-token-67890";
        List<String> trackingNumbers = List.of("GHNTESTING");

        stubFor(post(urlEqualTo("/shiip/public-api/v2/a5/gen-token"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "token": "%s"
                                    }
                                }
                                """.formatted(expectedToken))));

        // Act
        String result = ghnService.getPrintOrderUrl(trackingNumbers);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(expectedToken));
        assertTrue(result.contains("http://localhost:" + wireMockServer.port() + "/shiip/public-api/a5/gen-token?token="));
    }

    @Test
    @DisplayName("Integration test: Tạo đơn hàng và lấy link in - Flow hoàn chỉnh")
    void testCreateOrderAndGetPrintUrl_FullFlow() {
        // Arrange
        String expectedOrderCode = "GHNTESTING_FULL_FLOW";
        String expectedPrintToken = "full-flow-token-123";

        // Stub create order
        stubFor(post(urlEqualTo("/shiip/public-api/v2/shipping-order/create"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "order_code": "%s",
                                        "sort_code": "123-456-789",
                                        "trans_type": "fly",
                                        "total_fee": 30000,
                                        "expected_delivery_time": "2024-12-05T00:00:00Z"
                                    }
                                }
                                """.formatted(expectedOrderCode))));

        // Stub get print token
        stubFor(post(urlEqualTo("/shiip/public-api/v2/a5/gen-token"))
                .withHeader("Token", equalTo("test-token"))
                .withRequestBody(containing(expectedOrderCode))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": 200,
                                    "message": "Success",
                                    "data": {
                                        "token": "%s"
                                    }
                                }
                                """.formatted(expectedPrintToken))));

        CreateShippingOrder createRequest = createTestShippingOrderRequest();

        // Act - Tạo đơn hàng
        CreateOrderData createResult = ghnService.createShippingOrder(createRequest);

        // Act - Lấy link in đơn
        List<String> orderCodes = List.of(createResult.getOrderCode());
        String printUrl = ghnService.getPrintOrderUrl(orderCodes);

        // Assert
        assertNotNull(createResult);
        assertEquals(expectedOrderCode, createResult.getOrderCode());
        assertEquals(30000, createResult.getTotalFee());

        assertNotNull(printUrl);
        assertTrue(printUrl.contains(expectedPrintToken));
        assertTrue(printUrl.startsWith("http://localhost:" + wireMockServer.port()));
    }

    @Test
    @DisplayName("Kiểm tra timeout khi API không phản hồi")
    void testCreateShippingOrder_Timeout() {
        // Arrange
        stubFor(post(urlEqualTo("/shiip/public-api/v2/shipping-order/create"))
                .withHeader("Token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withFixedDelay(15000) // Delay 15s, timeout là 10s
                        .withStatus(200)));

        CreateShippingOrder request = createTestShippingOrderRequest();

        // Act & Assert
        ShippingServiceException exception = assertThrows(
                ShippingServiceException.class,
                () -> ghnService.createShippingOrder(request)
        );

        assertEquals("SHIPPING_API_ERROR", exception.getCode());
    }

    private CreateShippingOrder createTestShippingOrderRequest() {
        // Tạo test data
        GHNItem testItem = GHNItem.builder()
                .name("Test Product")
                .quantity(2)
                .price(100000)
                .weight(500)
                .build();

        ParcelInfor parcelInfor = ParcelInfor.builder()
                .weight(1000)
                .length(20)
                .width(20)
                .height(20)
                .codAmount(200000L)
                .build();

        return CreateShippingOrder.builder()
                .requiredNote("")
                .paymentTypeId(2)
                .serviceTypeId(2)
                .toName("Nguyen Van A")
                .toPhone("0123456789")
                .toAddress("123 Test Street")
                .toWardName("Phường 14")
                .toDistrictName("Quận 10")
                .toProvinceName("TP. Hồ Chí Minh")
                .parcelInfor(parcelInfor)
                .items(List.of(testItem))
                .build();
    }

    private Shipment createTestShipment() {
        // Mock objects for test
        ProductVariant variant = new ProductVariant();
        variant.setWeightGrams(500);

        OrderItem orderItem = new OrderItem();
        orderItem.setProductName("Test Product");
        orderItem.setUnitPriceAmount(100000L);
        orderItem.setVariant(variant);

        ShipmentItem shipmentItem = new ShipmentItem();
        shipmentItem.setOrderItem(orderItem);
        shipmentItem.setQuantity(2);

        Order order = new Order();
        order.setTotalAmount(200000L);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setItems(List.of(shipmentItem));

        return shipment;
    }


}