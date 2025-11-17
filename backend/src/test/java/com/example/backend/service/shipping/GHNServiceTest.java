package com.example.backend.service.shipping;

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
}
