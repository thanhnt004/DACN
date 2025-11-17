package com.example.backend.service.shipping;

import com.example.backend.dto.request.ghn.CalculateFeeRequest;
import com.example.backend.dto.request.ghn.GHNShopInfo;
import com.example.backend.dto.request.ghn.ParcelInfor;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNService {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ghn.api.base-url}")
    public String baseUrl;

    @Value("${ghn.api.token}")
    public String apiToken;

    @Value("${ghn.api.shop-id}")
    private Integer shopId;

    @Value("${ghn.defaults.timeout:15s}")
    public Duration timeout;
    @Value("${ghn.api.urls.services}")
    private  String getServiceApi;
    @Value("${ghn.api.urls.fee}")
    private  String calculateFeeApi ;
    @Value("${ghn.api.urls.leadtime}")
    private  String getExpectedDeliveryDateApi;
    @Value("${ghn.api.urls.provinces}")
    private  String getProvinceListApi;
    @Value("${ghn.api.urls.districts}")
    private  String getDistrictListApi;
    @Value("${ghn.api.urls.wards}")
    private  String getWardListApi ;
    @Value("${ghn.api.urls.shop-info}")
    private  String getShopInfoApi;
    public GHNShopInfo getShopDetails() {
        Map<String,Object> shopResponse = getShopInfo();
        var data = (Map<String, Object>) shopResponse.get("data");
        var shopData = ((List<Object>) data.get("shops")).get(0);
        return objectMapper.convertValue(shopData, GHNShopInfo.class);
    }
    public GHNWard getWard(String ward,String city, String district) {
        Integer districtId = getDistrictByName(city, district).getDistrictID();
        Map<String,Object> wardsResponse = getWards(districtId);
        var wardsData = (List<Map<String, Object>>) wardsResponse.get("data");
        for (Map<String, Object> w : wardsData) {
            List<String> wardNames = (List<String>) w.get("NameExtension");
            if (wardNames!=null&&wardNames.contains(ward)) {
                return objectMapper.convertValue(w, GHNWard.class);
            }
        }
        return null;
    }
    public GHNDistrict getDistrictByName(String city, String district) {
        Integer provinceId = getProvinceByName(city).getProvinceID();
        if (provinceId == null) {
            return null;
        }
        Map<String,Object> districtsResponse = getDistricts(provinceId);
        var districtsData = (List<Map<String, Object>>) districtsResponse.get("data");
        for (Map<String, Object> dist : districtsData) {
            List<String> districtNames = (List<String>) dist.get("NameExtension");
            if (districtNames!=null&&districtNames.contains(district)) {
                return objectMapper.convertValue(dist, GHNDistrict.class);
            }
        }
        return null;
    }
    public GHNProvince getProvinceByName(String city) {
        Map<String, Object> provincesResponse = getProvinces();
        var provincesData = (List<Map<String, Object>>) provincesResponse.get("data");
        for (Map<String, Object> province : provincesData) {
            List<String> provinceNames = (List<String>) province.get("NameExtension");
            if (provinceNames!=null&&provinceNames.contains(city)){
                if ((int)province.get("ProvinceID") >= 286) {
                    log.warn("GHN Province ID {} is deprecated", province.get("ProvinceID"));
                    continue;
                }
                log.info(province.toString());
                return objectMapper.convertValue(province, GHNProvince.class);
            }

        }
        return null;
    }

    public List<ShippingOption> getShippingOptions(List<CheckoutItemDetail> items, Optional<UserAddress> userAddress) {
        List<ShippingOption> result = new ArrayList<>();
        if (userAddress.isPresent())
        {
            GHNWard ward = getWard(
                    userAddress.get().getWard(),
                    userAddress.get().getProvince(),
                    userAddress.get().getDistrict()
            );
            log.info(ward.toString());
            if (ward.getStatus()==2)
            {
                return List.of(ShippingOption.builder()
                                .carrier("GHN")
                                .unavailableReason("GHN doesn't ship to this address")
                                .isAvailable(false)
                                .build());
            }
            Map<String, Object> serviceResponse = getAllService(ward.getDistrictID(),getShopDetails().getDistrict_id());
            List<Map<String,Object>> availableServices = (List<Map<String, Object>>) serviceResponse.get("data");
            for(Map service:availableServices)
            {
                if ((int)service.get("service_type_id") == 5)
                    continue;
                int shippingFee = calculateShippingFee(items, ward, String.valueOf(service.get("service_type_id")));
                int leadTime = getLeadTime(ward, String.valueOf(service.get("service_type_id")));
                ShippingOption option = ShippingOption.builder()
                        .id(String.valueOf(service.get("service_type_id")))
                        .name((String) service.get("short_name"))
                        .carrier("GHN")
                        .description((String) service.get("short_name"))
                        .isAvailable(true)
                        .isDefault(false)
                        .amount(shippingFee)
                        .estimatedDays(leadTime)
                        .build();
                result.add(option);
            }
            return result;
        }else {
            return List.of(ShippingOption.builder()
                    .carrier("GHN")
                    .unavailableReason("No shipping address provided")
                    .isAvailable(false)
                    .build());
        }
    }
    public int calculateShippingFee(List<CheckoutItemDetail> items, GHNWard ward, String serviceTypeId) {
        // Tính tổng trọng lượng và giá trị đơn hàng
        int totalWeight = items.stream().mapToInt(CheckoutItemDetail::getWeight).sum();
        long totalPrice = items.stream().mapToLong(CheckoutItemDetail::getTotalAmount).sum();

        double volKg = (20 * 20 * 20) / 5000.0;   // kg
        int volGrams = (int) Math.ceil(volKg * 1000.0);     // convert to grams and round up
        int finalWeight = Math.max(totalWeight, volGrams);
        // Tạo đối tượng CalculateFeeRequest
        CalculateFeeRequest calculateFeeRequest = CalculateFeeRequest.builder()
                .toWardCode(ward.getWardCode())
                .toDistrictId(ward.getDistrictID())
                .serviceTypeId(Integer.parseInt(serviceTypeId))
                .parcelInfor(ParcelInfor.builder()
                        .weight(finalWeight)
                        .length(20) // Chiều dài gói hàng (cm)
                        .width(20)  // Chiều rộng gói hàng (cm)
                        .height(20) // Chiều cao gói hàng (cm)
                        .codAmount(totalPrice)
                        .build())
                .build();

        Map<String,Object> feeResponse = calculateFee(calculateFeeRequest);
        Map<String,Object> feeData = (Map<String, Object>) feeResponse.get("data");
        return (int) feeData.get("total");
    }
    public int getLeadTime(GHNWard ward, String serviceTypeId)
    {
        Map<String,Object> leadTimeResponse = getExpectedDeliveryDate(getShopDetails(),ward,serviceTypeId);
        Map<String,Object> leadTimeData = (Map<String, Object>) leadTimeResponse.get("data");
        return (int) leadTimeData.get("leadtime");
    }
    //
    private Map<String,Object> getExpectedDeliveryDate(GHNShopInfo shopInfo,GHNWard ward, String serviceTypeId) {
        log.debug("Fetching expected delivery date to {}", ward.getDistrictID());

        try {
            Map<String, Object> requestBody = Map.of(
                    "shop_id", shopInfo.get_id(),
                    "from_district_id", shopInfo.getDistrict_id(),
                    "to_district_id", ward.getDistrictID(),
                    "to_ward_code", ward.getWardCode(),
                    "service_type_id", Integer.parseInt(serviceTypeId)
            );
            return webClientBuilder.build()
                    .post()
                    .uri(getExpectedDeliveryDateApi)
                    .header("Token", apiToken)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch wards", e);
            throw new GhnApiException("Không thể lấy danh sách phường/xã", e);
        }
    }
    private Map<String,Object> getShopInfo() {
        log.debug("Fetching shop info from GHN");

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(getShopInfoApi)
                    .header("Token", apiToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch shop info", e);
            throw new GhnApiException("Không thể lấy thông tin cửa hàng", e);
        }
    }
    private Map<String,Object> calculateFee(CalculateFeeRequest calculateFeeRequest) {
        log.debug("Fetching all services to {}", calculateFeeRequest.getToDistrictId());

        try {
            log.info(objectMapper.convertValue(calculateFeeRequest,Map.class).toString());
            return webClientBuilder.build()
                    .post()
                    .uri(calculateFeeApi)
                    .header("Token", apiToken)
                    .header("ShopId",shopId.toString())
                    .bodyValue(calculateFeeRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch money", e);
            throw new GhnApiException("Không thể tính tiền ", e);
        }
    }

    private Map<String,Object> getAllService(Integer toDistrictId,Integer fromDistrictId) {
        log.debug("Fetching all services to {}", toDistrictId);

        try {
            Map<String, Object> requestBody = Map.of(
                    "shop_id", shopId,
                    "from_district",fromDistrictId,
                    "to_district", toDistrictId
            );
            return webClientBuilder.build()
                    .post()
                    .uri(getServiceApi)
                    .header("Token", apiToken)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch wards", e);
            throw new GhnApiException("Không lấy danh sách service", e);
        }
    }
    public Map<String, Object> getProvinces() {
        log.debug("Fetching provinces from GHN");

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(getProvinceListApi)
                    .header("Token", apiToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch provinces", e);
            throw new GhnApiException("Không thể lấy danh sách tỉnh/thành", e);
        }
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    Map<String,Object> getDistricts(Integer provinceId) {
        log.debug("Fetching districts for province: {}", provinceId);

        try {
            return webClientBuilder.build()
                    .post()
                    .uri(getDistrictListApi)
                    .header("Token", apiToken)
                    .bodyValue(Map.of("province_id", provinceId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch districts", e);
            throw new GhnApiException("Không thể lấy danh sách quận/huyện", e);
        }
    }

    /**
     * Lấy danh sách phường/xã theo quận
     */
    Map<String,Object> getWards(Integer districtId) {
        log.debug("Fetching wards for district: {}", districtId);
        Map<String, Object> body = Map.of("district_id", districtId);
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(getWardListApi)
                    .header("Token", apiToken)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch wards", e);
            throw new GhnApiException("Không thể lấy danh sách phường/xã", e);
        }
    }
}
