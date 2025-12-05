package com.example.backend.service.shipping;


import com.example.backend.dto.ghn.GHNShopInfo;

import com.example.backend.dto.request.checkout.CheckOutItem;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@Slf4j
@SpringBootTest
class GHNServiceTest {
    @Autowired
    private GHNService ghnService;


    @BeforeEach
    void ensureConfigPresent() {

    }
    @Test
    void callGetProvinces_andFindByName() {
        // Thử tìm tỉnh Hà Nội (tùy dữ liệu GHN trả về)
        GHNProvince province = ghnService.getProvinceByName("Hà Nội");
        assertNotNull(province, "Expect GHN to return Hà Nội province");
        System.out.println("Found province: " + province);
    }
    @Test
    void callGetDistricts_andFindByName() {
        // Thử tìm tỉnh Hà Nội (tùy dữ liệu GHN trả về)
        GHNDistrict district = ghnService.getDistrictByName("Hà Nội","Ba Đình");
        assertNotNull(district, "Expect GHN to return Hà Nội province");
        System.out.println("Found province: " + district);
    } @Test
    void callWard_andFindByName() {
        // Thử tìm tỉnh Hà Nội (tùy dữ liệu GHN trả về)
        GHNWard ward = ghnService.getWard("P.Vĩnh Phúc","Hà Nội","Ba Đình");
        assertNotNull(ward, "Expect GHN to return Hà Nội province");
        System.out.println("Found province: " + ward);
    }
    @Test
    void callGetDistrict_listNotEmpty() {
        var districtResponse = ghnService.getDistricts(202);// phương thức private trong class gốc? nếu là private, bạn có thể gọi getProvinceByName hoặc expose helper
        assertNotNull(districtResponse);
        log.info("District Response: {}", districtResponse);
        var data = districtResponse.get("data");
        assertNotNull(data, "Expected data key in GHN response");
        assertTrue(((java.util.List<?>) data).size() > 0, "Expected at least one province from GHN");
        System.out.println("District size = " + ((java.util.List<?>) data).size());
    }
    @Test
    void callGetWard_listNotEmpty() {
        var ward = ghnService.getWards(1484);// phương thức private trong class gốc? nếu là private, bạn có thể gọi getProvinceByName hoặc expose helper
        assertNotNull(ward);
        log.info("ward Response: {}", ward);
        var data = ward.get("data");
        assertNotNull(data, "Expected data key in GHN response");
        assertTrue(((java.util.List<?>) data).size() > 0, "Expected at least one ward from GHN");
        System.out.println("ward size = " + ((java.util.List<?>) data).size());
    }
    @Test
    void callGetProvinces_listNotEmpty() {
        // Gọi trực tiếp service nội bộ để kiểm tra response từ GHN
        var provincesResponse = ghnService.getProvinces();// phương thức private trong class gốc? nếu là private, bạn có thể gọi getProvinceByName hoặc expose helper
        assertNotNull(provincesResponse);
        var data = provincesResponse.get("data");
        assertNotNull(data, "Expected data key in GHN response");
        assertTrue(((java.util.List<?>) data).size() > 0, "Expected at least one province from GHN");
        System.out.println("Provinces size = " + ((java.util.List<?>) data).size());
    }
    @Test
    void calculateShippingFee_success() {
        // ARRANGE: Tạo danh sách items để tính phí
        CheckoutItemDetail item1 = CheckoutItemDetail.builder()
                .quantity(2)
                .weight(500)  // 500g mỗi item
                .totalAmount(500000L)  // 500,000 VND
                .build();

        CheckoutItemDetail item2 = CheckoutItemDetail.builder()
                .quantity(1)
                .weight(300)  // 300g
                .totalAmount(300000L)  // 300,000 VND
                .build();

        List<CheckoutItemDetail> items = List.of(item1, item2);

        // Lấy thông tin ward đích (Phường Vĩnh Phúc, Quận Ba Đình, Hà Nội)
        GHNWard ward = ghnService.getWard("P.Vĩnh Phúc", "Hà Nội", "Ba Đình");
        assertNotNull(ward, "Ward should not be null");

        // ACT: Tính phí vận chuyển với service type 2 (giao hàng tiêu chuẩn)
        int shippingFee = ghnService.calculateShippingFee(items, ward, "2");

        // ASSERT: Kiểm tra kết quả
        assertTrue(shippingFee > 0, "Shipping fee should be greater than 0");

        log.info("=== SHIPPING FEE CALCULATION ===");
        log.info("Items: {} items", items.size());
        log.info("Total weight: {}g", items.stream().mapToInt(CheckoutItemDetail::getWeight).sum());
        log.info("Total amount: {}đ", items.stream().mapToLong(CheckoutItemDetail::getTotalAmount).sum());
        log.info("Destination: Ward {}, District {}", ward.getWardCode(), ward.getDistrictID());
        log.info("Service Type: 2 (Standard)");
        log.info("💰 Calculated Shipping Fee: {}đ", shippingFee);
        log.info("================================");
    }
    @Test
    void getLeadtime_success() {
        // Thử lấy thời gian vận chuyển từ Hà Nội (202) đến Quận Ba Đình (1484)
        int leadtime = ghnService.getLeadTime(ghnService.getWard("P.Vĩnh Phúc","Hà Nội","Ba Đình"),"0");
        assertTrue(leadtime > 0, "Expected leadtime to be greater than 0");
        System.out.println("Calculated leadtime: " + leadtime);
    }
    @Test
    void getShippingOptions()
    {
        CheckoutItemDetail checkOutItem = CheckoutItemDetail.builder()
                .quantity(2)
                .weight(500)
                .totalAmount(500000L)
                .build();
        UserAddress userAddress = new UserAddress();
        userAddress.setWard("P.Vĩnh Phúc");
        userAddress.setProvince("Hà Nội");
        userAddress.setDistrict("Ba Đình");
        userAddress.setPhone("0326725877");
        List<ShippingOption> shippingOptions = ghnService.getShippingOptions(List.of(checkOutItem), Optional.of(userAddress));
        assertNotNull(shippingOptions, "Expect GHN to return Hà Nội province");
        System.out.println("Found province: " + shippingOptions);
    }

    /**
     * Integration Test: Tạo đơn hàng GHN thật
     * Note: Test này sẽ tạo đơn hàng thật trên hệ thống GHN
     */
    @Test
    void testGetShopInfo() {
        // Test để kiểm tra thông tin shop và warehouse
        GHNShopInfo shopInfo = ghnService.getShopDetails();
        assertNotNull(shopInfo, "Shop info should not be null");

        log.info("=== SHOP INFORMATION ===");
        log.info("Shop ID: {}", shopInfo.get_id());
        log.info("Shop Name: {}", shopInfo.getName());
        log.info("Shop Phone: {}", shopInfo.getPhone());
        log.info("Shop Address: {}", shopInfo.getAddress());
        log.info("Shop District ID: {}", shopInfo.getDistrict_id());
        log.info("Shop Ward Code: {}", shopInfo.getWard_code());
        log.info("Shop Status: {}", shopInfo.getStatus());
        log.info("========================");
    }

    @Test
    void createRealShippingOrder_Success() {
        // ARRANGE: Lấy thông tin shop
        GHNShopInfo shopInfo = ghnService.getShopDetails();

        // Tạo đơn hàng test với format giống curl thành công
        com.example.backend.dto.ghn.CreateShippingOrder createOrderRequest =
            com.example.backend.dto.ghn.CreateShippingOrder.builder()
                .clientOrderCode("TEST-" + System.currentTimeMillis())
                .requiredNote("KHONGCHOXEMHANG")
                .paymentTypeId(2) // Người gửi trả phí
                .serviceTypeId(2) // Giao hàng tiêu chuẩn
                .content("Test order from Spring Boot")

                // Địa chỉ người gửi (shop) - CHỈ DÙNG TEXT NAME theo doc GHN
                .fromName(shopInfo.getName())
                .fromPhone(shopInfo.getPhone())
                .fromAddress(shopInfo.getAddress())
                .fromWardName("Phường 14")
                .fromDistrictName("Quận 10")
                .fromProvinceName("TP. Hồ Chí Minh")

                // Địa chỉ người nhận - DÙNG TEXT NAME
                .toName("Nguyễn Văn Test")
                .toPhone("0326725877")
                .toAddress("72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam")
                .toWardName("Phường 14")
                .toDistrictName("Quận 10")
                .toProvinceName("TP. Hồ Chí Minh")

                .parcelInfor(com.example.backend.dto.ghn.ParcelInfor.builder()
                    .weight(500)
                    .length(12)
                    .width(12)
                    .height(12)
                    .codAmount(200000L)
                    .build())
                .items(List.of(
                    com.example.backend.dto.ghn.GHNItem.builder()
                        .name("Áo Polo")
                        .quantity(1)
                        .price(200000)
                        .weight(500)
                        .build()
                ))
                .build();

        log.info("📤 Request to GHN API:");
        log.info("  Shop ID: {} (sent via header)", shopInfo.get_id());
        log.info("  From: {} - {} (Shop)", createOrderRequest.getFromName(), createOrderRequest.getFromPhone());
        log.info("  From Address: {}", createOrderRequest.getFromAddress());
        log.info("  To: {} - {}", createOrderRequest.getToName(), createOrderRequest.getToPhone());
        log.info("  To Address: {}", createOrderRequest.getToAddress());
        log.info("  To Ward/District/Province: {}/{}/{}",
            createOrderRequest.getToWardName(),
            createOrderRequest.getToDistrictName(),
            createOrderRequest.getToProvinceName());
        log.info("  Service Type: {}, Payment Type: {}", createOrderRequest.getServiceTypeId(), createOrderRequest.getPaymentTypeId());
        log.info("  Weight: {}g, COD: {}", createOrderRequest.getParcelInfor().getWeight(), createOrderRequest.getParcelInfor().getCodAmount());

        // ACT: Gọi API tạo đơn hàng thật
        com.example.backend.dto.ghn.CreateOrderData result = ghnService.createShippingOrder(createOrderRequest);

        // ASSERT: Kiểm tra kết quả
        assertNotNull(result, "Create order result should not be null");
        assertNotNull(result.getOrderCode(), "Order code should not be null");
        assertNotNull(result.getExpectedDeliveryTime(), "Expected delivery time should not be null");
        assertTrue(result.getTotalFee() > 0, "Total fee should be greater than 0");

        log.info("✅ Created GHN Order Successfully!");
        log.info("📦 Order Code: {}", result.getOrderCode());
        log.info("💰 Total Fee: {}", result.getTotalFee());
        log.info("📅 Expected Delivery: {}", result.getExpectedDeliveryTime());
    }

    /**
     * Integration Test: Lấy link in đơn hàng GHN
     * Note: Cần tạo đơn hàng trước khi chạy test này
     */
    @Test
    void getPrintOrderUrl_Success() {
        // ARRANGE: Tạo đơn hàng trước
        GHNShopInfo shopInfo = ghnService.getShopDetails();
        com.example.backend.dto.ghn.CreateShippingOrder createOrderRequest =
                com.example.backend.dto.ghn.CreateShippingOrder.builder()
                        .clientOrderCode("TEST-" + System.currentTimeMillis())
                        .requiredNote("KHONGCHOXEMHANG")
                        .paymentTypeId(2) // Người gửi trả phí
                        .serviceTypeId(2) // Giao hàng tiêu chuẩn
                        .content("Test order from Spring Boot")

                        // Địa chỉ người gửi (shop) - CHỈ DÙNG TEXT NAME theo doc GHN
                        .fromName(shopInfo.getName())
                        .fromPhone(shopInfo.getPhone())
                        .fromAddress(shopInfo.getAddress())
                        .fromWardName("Phường 14")
                        .fromDistrictName("Quận 10")
                        .fromProvinceName("TP. Hồ Chí Minh")

                        // Địa chỉ người nhận - DÙNG TEXT NAME
                        .toName("Nguyễn Văn Test")
                        .toPhone("0326725877")
                        .toAddress("72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam")
                        .toWardName("Phường 14")
                        .toDistrictName("Quận 10")
                        .toProvinceName("TP. Hồ Chí Minh")

                        .parcelInfor(com.example.backend.dto.ghn.ParcelInfor.builder()
                                .weight(500)
                                .length(12)
                                .width(12)
                                .height(12)
                                .codAmount(200000L)
                                .build())
                        .items(List.of(
                                com.example.backend.dto.ghn.GHNItem.builder()
                                        .name("Áo Polo")
                                        .quantity(1)
                                        .price(200000)
                                        .weight(500)
                                        .build()
                        ))
                        .build();

        com.example.backend.dto.ghn.CreateOrderData orderResult = ghnService.createShippingOrder(createOrderRequest);
        assertNotNull(orderResult.getOrderCode(), "Order code must exist before getting print URL");

        // ACT: Lấy link in đơn
        String printUrl = ghnService.getPrintOrderUrl(List.of(orderResult.getOrderCode()));

        // ASSERT: Kiểm tra kết quả
        assertNotNull(printUrl, "Print URL should not be null");
        assertTrue(printUrl.contains("http"), "Print URL should be a valid URL");

        log.info("✅ Got Print Order URL Successfully!");
        log.info("📦 Order Code: {}", orderResult.getOrderCode());
        log.info("🖨️ Print URL: {}", printUrl);
        log.info("💡 Tip: Copy this URL to browser to view/print the order");
    }

    /**
     * Integration Test: Tạo nhiều đơn hàng và in hàng loạt
     */
    @Test
    void createMultipleOrders_AndGetBatchPrintUrl() {
        List<String> orderCodes = new ArrayList<>();
        GHNShopInfo shopInfo = ghnService.getShopDetails();

        // ARRANGE & ACT: Tạo 3 đơn hàng
        for (int i = 1; i <= 3; i++) {
            com.example.backend.dto.ghn.CreateShippingOrder createOrderRequest =
                com.example.backend.dto.ghn.CreateShippingOrder.builder()
                    .clientOrderCode("BATCH-TEST-" + System.currentTimeMillis() + "-" + i)
                    .requiredNote("KHONGCHOXEMHANG")
                    .paymentTypeId(2)
                    .serviceTypeId(2)
                    .content("Batch order test " + i)

                    // Địa chỉ người gửi (shop) - DÙNG TEXT NAME
                    .fromName(shopInfo.getName())
                    .fromPhone(shopInfo.getPhone())
                    .fromAddress(shopInfo.getAddress())
                    .fromWardName("Phường 14")
                    .fromDistrictName("Quận 10")
                    .fromProvinceName("TP. Hồ Chí Minh")

                    // Địa chỉ người nhận - DÙNG TEXT NAME
                    .toName("Khách hàng số " + i)
                    .toPhone("0326725877")
                    .toAddress("72 Thành Thái, Phường 14, Quận 10, Hồ Chí Minh, Vietnam")
                    .toWardName("Phường 14")
                    .toDistrictName("Quận 10")
                    .toProvinceName("TP. Hồ Chí Minh")

                    .parcelInfor(com.example.backend.dto.ghn.ParcelInfor.builder()
                        .weight(200 * i)
                        .length(10 + i)
                        .width(10 + i)
                        .height(10 + i)
                        .codAmount(100000L * i)
                        .build())
                    .items(List.of(
                        com.example.backend.dto.ghn.GHNItem.builder()
                            .name("Sản phẩm batch " + i)
                            .quantity(1)
                            .price(100000 * i)
                            .weight(200 * i)
                            .build()
                    ))
                    .build();

            com.example.backend.dto.ghn.CreateOrderData result = ghnService.createShippingOrder(createOrderRequest);
            assertNotNull(result.getOrderCode());
            orderCodes.add(result.getOrderCode());

            log.info("📦 Created order {}/3: {}", i, result.getOrderCode());
        }

        // ACT: Lấy link in hàng loạt
        String batchPrintUrl = ghnService.getPrintOrderUrl(orderCodes);

        // ASSERT
        assertNotNull(batchPrintUrl, "Batch print URL should not be null");
        assertTrue(batchPrintUrl.contains("http"), "Batch print URL should be a valid URL");
        assertEquals(3, orderCodes.size(), "Should have created 3 orders");

        log.info("✅ Created {} orders successfully!", orderCodes.size());
        log.info("📋 Order Codes: {}", orderCodes);
        log.info("🖨️ Batch Print URL: {}", batchPrintUrl);
        log.info("💡 Use this URL to print all orders at once");
    }

    /**
     * Integration Test: Tạo đơn hoàn hàng (Return Order)
     * Note: Đơn hoàn hàng là đơn gửi từ khách về shop
     */
    @Test
    void createReturnShippingOrder_Success() {
        // ARRANGE: Tạo đơn hoàn hàng
        com.example.backend.model.order.Shipment mockShipment = new com.example.backend.model.order.Shipment();
        com.example.backend.model.order.Order mockOrder = new com.example.backend.model.order.Order();
        mockOrder.setTotalAmount(400000L);
        mockShipment.setOrder(mockOrder);

        com.example.backend.model.order.OrderItem mockOrderItem = new com.example.backend.model.order.OrderItem();
        mockOrderItem.setProductName("Sản phẩm hoàn trả");
        mockOrderItem.setUnitPriceAmount(400000L);

        com.example.backend.model.product.ProductVariant mockVariant = new com.example.backend.model.product.ProductVariant();
        mockVariant.setWeightGrams(400);
        mockOrderItem.setVariant(mockVariant);

        com.example.backend.model.order.ShipmentItem mockShipmentItem = new com.example.backend.model.order.ShipmentItem();
        mockShipmentItem.setOrderItem(mockOrderItem);
        mockShipmentItem.setQuantity(1);

        mockShipment.setItems(List.of(mockShipmentItem));

        UserAddress customerAddress = new UserAddress();
        customerAddress.setFullName("Khách hàng hoàn trả");
        customerAddress.setPhone("0326725877");
        customerAddress.setLine1("789 Đường hoàn trả");
        customerAddress.setWard("P.Vĩnh Phúc");
        customerAddress.setProvince("Hà Nội");
        customerAddress.setDistrict("Ba Đình");

        // ACT: Build request và tạo đơn hoàn hàng
        com.example.backend.dto.ghn.CreateShippingOrder returnOrderRequest =
            ghnService.buildCreateOrderRequest(mockShipment, customerAddress, true);

        com.example.backend.dto.ghn.CreateOrderData result = ghnService.createShippingOrder(returnOrderRequest);

        // ASSERT
        assertNotNull(result, "Return order result should not be null");
        assertNotNull(result.getOrderCode(), "Return order code should not be null");

        log.info("✅ Created Return Order Successfully!");
        log.info("📦 Return Order Code: {}", result.getOrderCode());
        log.info("💰 Return Fee: {}", result.getTotalFee());
        log.info("🔄 This is a return order from customer to shop");
    }
}
