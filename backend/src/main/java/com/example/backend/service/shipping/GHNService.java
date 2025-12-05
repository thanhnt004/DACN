package com.example.backend.service.shipping;

import com.example.backend.dto.ghn.*;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.shipping.GhnDistrictOption;
import com.example.backend.dto.response.shipping.GhnProvinceOption;
import com.example.backend.dto.response.shipping.GhnWardOption;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.shipping.ShippingServiceException;
import com.example.backend.model.order.Shipment;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNService {
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final com.example.backend.config.CacheConfig cacheConfig;

    @Value("${ghn.api.base-url}")
    public String baseUrl;

    @Value("${ghn.api.token}")
    public String apiToken;

    @Value("${ghn.api.shop-id}")
    private Integer shopId;

    @Value("${ghn.defaults.timeout:15s}")
    public Duration timeout;
    @Value("${ghn.api.urls.services}")
    private String getServiceApi;
    @Value("${ghn.api.urls.fee}")
    private String calculateFeeApi;
    @Value("${ghn.api.urls.leadtime}")
    private String getExpectedDeliveryDateApi;
    @Value("${ghn.api.urls.provinces}")
    private String getProvinceListApi;
    @Value("${ghn.api.urls.districts}")
    private String getDistrictListApi;
    @Value("${ghn.api.urls.wards}")
    private String getWardListApi;
    @Value("${ghn.api.urls.shop-info}")
    private String getShopInfoApi;
    @Value("${ghn.api.urls.create-order}")
    private String createOrderApi;
    @Value("${ghn.api.urls.print-order}")
    private String getPrintOrderApi;
    @Value("${ghn.api.urls.print-token}")
    private String getPrintTokenApi;
    @Value("${ghn.api.urls.cancel-order}")
    private String cancelOrderApi;
    //default
    @Value("${ghn.defaults.required-note:}")
    private String requiredNote;
    @Value("${ghn.defaults.payment-type-id:2}")
    private int paymentTypeId;
    @Value("${ghn.defaults.service-type-id:2}")
    private int serviceTypeId;
    public CreateShippingOrder buildCreateOrderRequest(Shipment shipment, UserAddress userAddress, boolean isReturn) {
        List<GHNItem> ghnItems = shipment.getItems().stream()
                .map(item -> GHNItem.builder()
                        .name(item.getOrderItem().getProductName())
                        .quantity(item.getQuantity())
                        .price(Math.toIntExact(item.getOrderItem().getUnitPriceAmount()))
                        .weight(item.getOrderItem().getVariant().getWeightGrams())
                        .build())
                .collect(Collectors.toList());

        GHNShopInfo shopInfo = getShopDetails();

        ParcelInfor parcelInfor = ParcelInfor.builder()
                .weight(ghnItems.stream().mapToInt(GHNItem::getWeight).sum())
                .length(20)
                .width(20)
                .height(20)
                .codAmount(shipment.getOrder().getTotalAmount())
                .build();

        if (!isReturn) {
            // Đơn giao hàng bình thường: từ shop → khách hàng
            // Lấy thông tin địa chỉ shop để get ward/district/province name
            GHNWard shopWard = getWardByCode(shopInfo.getWard_code(), shopInfo.getDistrict_id());
            GHNDistrict shopDistrict = getDistrictById(shopInfo.getDistrict_id());
            GHNProvince shopProvince = getProvinceById(shopInfo.getProvince_id());

            return CreateShippingOrder.builder()
                .requiredNote(requiredNote)
                .paymentTypeId(paymentTypeId)
                .serviceTypeId(serviceTypeId)

                // Địa chỉ người nhận (khách hàng) - sử dụng tên
                .toName(userAddress.getFullName())
                .toPhone(userAddress.getPhone())
                .toAddress(userAddress.getAddressLine())
                .toWardName(userAddress.getWard())
                .toDistrictName(userAddress.getDistrict())
                .toProvinceName(userAddress.getProvince())

                // Địa chỉ người gửi (shop) - CHỈ DÙNG TEXT NAME theo GHN doc
                .fromName(shopInfo.getName())
                .fromPhone(shopInfo.getPhone())
                .fromAddress(shopInfo.getAddress())
                .fromWardName(shopWard != null ? shopWard.getWardName() : "")
                .fromDistrictName(shopDistrict != null ? shopDistrict.getDistrictName() : "")
                .fromProvinceName(shopProvince != null ? shopProvince.getProvinceName() : "")

                .parcelInfor(parcelInfor)
                .items(ghnItems)
                .build();
        } else {
            // Đơn hoàn hàng: từ khách hàng → shop
            // Lấy thông tin địa chỉ shop để get ward/district/province name
            GHNWard shopWard = getWardByCode(shopInfo.getWard_code(), shopInfo.getDistrict_id());
            GHNDistrict shopDistrict = getDistrictById(shopInfo.getDistrict_id());
            GHNProvince shopProvince = getProvinceById(shopInfo.getProvince_id());

            return CreateShippingOrder.builder()
                .requiredNote(requiredNote)
                .paymentTypeId(paymentTypeId)
                .serviceTypeId(serviceTypeId)

                // Địa chỉ người nhận (shop) - DÙNG TEXT NAME
                .toName(shopInfo.getName())
                .toPhone(shopInfo.getPhone())
                .toAddress(shopInfo.getAddress())
                .toWardName(shopWard != null ? shopWard.getWardName() : "")
                .toDistrictName(shopDistrict != null ? shopDistrict.getDistrictName() : "")
                .toProvinceName(shopProvince != null ? shopProvince.getProvinceName() : "")

                // Địa chỉ người gửi (khách hàng) - DÙNG TEXT NAME
                .fromName(userAddress.getFullName())
                .fromPhone(userAddress.getPhone())
                .fromAddress(userAddress.getAddressLine())
                .fromWardName(userAddress.getWard())
                .fromDistrictName(userAddress.getDistrict())
                .fromProvinceName(userAddress.getProvince())
                    .parcelInfor(parcelInfor)
                    .items(ghnItems)
                    .build();
        }

    }
    public CreateOrderData createShippingOrder(CreateShippingOrder request) {
        try {
            log.debug("Creating GHN shipping order with request: {}", request);
            log.debug("Using shopId: , token: {}***", apiToken.substring(0, Math.min(apiToken.length(), 10)));

            var response =  webClientBuilder.build()
                    .post()
                    .uri(createOrderApi)
                    .header("Token", apiToken)
                    .header("ShopId", shopId.toString())
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        log.error("GHN API Error Response: {}", body);
                                        return Mono.error(
                                                new ShippingServiceException("SHIPPING_API_ERROR", body)
                                        );
                                    })
                    )
                    .bodyToMono(new ParameterizedTypeReference<GhnResponse<CreateOrderData>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (response == null) {
                throw new ShippingServiceException("SHIPPING_API_ERROR", "GHN API returned null response");
            }
            return response.getData();
        } catch (ShippingServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create shipping order", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể tạo đơn vận chuyển: " + e.getMessage());
        }

    }
    /**
     * (Tùy chọn) Lấy Token in phiếu nếu cần tích hợp iframe
     */
    public String getPrintToken(List<String> orderCodes) {
        var body = java.util.Map.of("order_codes", orderCodes);
        try {
            var response =  webClientBuilder.build()
                    .post()
                    .uri(getPrintTokenApi)
                    .header("Token", apiToken)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return (String) data.get("token");
            } else {
                throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể tạo print token");
            }
        } catch (Exception e) {
            log.error("Failed to gen print token", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể tạo print token");
        }
    }
    @SuppressWarnings("unchecked")
    public GHNShopInfo getShopDetails() {
        Map<String, Object> shopResponse = getShopInfo();
        var data = (Map<String, Object>) shopResponse.get("data");
        var shopData = ((List<Object>) data.get("shops")).get(0);
        return objectMapper.convertValue(shopData, GHNShopInfo.class);
    }

    @SuppressWarnings("unchecked")
    public GHNWard getWard(String ward, String city, String district) {
        GHNDistrict matchedDistrict = getDistrictByName(city, district);
        if (matchedDistrict == null) {
            throw new ShippingServiceException("SHIPPING_DISTRICT_NOT_FOUND", "Không tìm thấy quận/huyện cho địa chỉ: " + district + ", " + city);
        }
        Integer districtId = matchedDistrict.getDistrictID();
        Map<String, Object> wardsResponse = getWards(districtId);
        var wardsData = (List<Map<String, Object>>) wardsResponse.get("data");
        for (Map<String, Object> w : wardsData) {
            List<String> wardNames = (List<String>) w.get("NameExtension");
            String wardName = (String) w.get("WardName");
            if (matchesName(ward, wardNames, wardName)) {
                return objectMapper.convertValue(w, GHNWard.class);
            }
        }
        throw new ShippingServiceException("SHIPPING_WARD_NOT_FOUND", "Không tìm thấy phường/xã: " + ward + ", " + district + ", " + city);
    }

    @SuppressWarnings("unchecked")
    public GHNDistrict getDistrictByName(String city, String district) {
        GHNProvince province = getProvinceByName(city);
        if (province == null) {
            log.warn("Province not found for city={} (district={})", city, district);
            return null;
        }
        Map<String, Object> districtsResponse = getDistricts(province.getProvinceID());
        var districtsData = (List<Map<String, Object>>) districtsResponse.get("data");
        for (Map<String, Object> dist : districtsData) {
            List<String> districtNames = (List<String>) dist.get("NameExtension");
            String districtName = (String) dist.get("DistrictName");
            if (matchesName(district, districtNames, districtName)) {
                return objectMapper.convertValue(dist, GHNDistrict.class);
            }
        }
        log.warn("District not found: city={}, district={}", city, district);
        return null;
    }

    @SuppressWarnings("unchecked")
    public GHNProvince getProvinceByName(String city) {
        Map<String, Object> provincesResponse = getProvinces();
        var provincesData = (List<Map<String, Object>>) provincesResponse.get("data");
        for (Map<String, Object> province : provincesData) {
            List<String> provinceNames = (List<String>) province.get("NameExtension");
            if (matchesName(city, provinceNames, (String) province.get("ProvinceName"))) {
                if ((int) province.get("ProvinceID") >= 286) {
                    log.warn("GHN Province ID {} is deprecated", province.get("ProvinceID"));
                    continue;
                }
                log.info(province.toString());
                return objectMapper.convertValue(province, GHNProvince.class);
            }

        }
        return null;
    }

    /**
     * Lấy thông tin ward theo ward code và district ID
     */
    @SuppressWarnings("unchecked")
    public GHNWard getWardByCode(String wardCode, int districtId) {
        if (wardCode == null || wardCode.isBlank()) {
            log.warn("Ward code is null or empty");
            return null;
        }
        
        try {
            Map<String, Object> wardsResponse = getWards(districtId);
            var wardsData = (List<Map<String, Object>>) wardsResponse.get("data");
            
            for (Map<String, Object> w : wardsData) {
                String code = (String) w.get("WardCode");
                if (wardCode.equals(code)) {
                    return objectMapper.convertValue(w, GHNWard.class);
                }
            }
            
            log.warn("Ward not found with code: {} in district: {}", wardCode, districtId);
            return null;
        } catch (Exception e) {
            log.error("Error getting ward by code: {}", wardCode, e);
            return null;
        }
    }

    /**
     * Lấy thông tin district theo district ID
     */
    @SuppressWarnings("unchecked")
    public GHNDistrict getDistrictById(int districtId) {
        try {
            // Lấy tất cả provinces để tìm province chứa district này
            List<GHNProvince> provinces = listProvincesRaw();
            
            for (GHNProvince province : provinces) {
                Map<String, Object> districtsResponse = getDistricts(province.getProvinceID());
                var districtsData = (List<Map<String, Object>>) districtsResponse.get("data");
                
                for (Map<String, Object> dist : districtsData) {
                    int id = (int) dist.get("DistrictID");
                    if (id == districtId) {
                        return objectMapper.convertValue(dist, GHNDistrict.class);
                    }
                }
            }
            
            log.warn("District not found with ID: {}", districtId);
            return null;
        } catch (Exception e) {
            log.error("Error getting district by ID: {}", districtId, e);
            return null;
        }
    }

    /**
     * Lấy thông tin province theo province ID
     */
    @SuppressWarnings("unchecked")
    public GHNProvince getProvinceById(int provinceId) {
        try {
            Map<String, Object> provincesResponse = getProvinces();
            var provincesData = (List<Map<String, Object>>) provincesResponse.get("data");
            
            for (Map<String, Object> province : provincesData) {
                int id = (int) province.get("ProvinceID");
                if (id == provinceId) {
                    return objectMapper.convertValue(province, GHNProvince.class);
                }
            }
            
            log.warn("Province not found with ID: {}", provinceId);
            return null;
        } catch (Exception e) {
            log.error("Error getting province by ID: {}", provinceId, e);
            return null;
        }
    }

    public List<GhnProvinceOption> getProvinceOptions() {
        return listProvincesRaw().stream()
                .filter(province -> province.getProvinceID() < 286)
                .map(province -> new GhnProvinceOption(province.getProvinceID(), province.getProvinceName()))
                .sorted(Comparator.comparing(GhnProvinceOption::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<GhnDistrictOption> getDistrictOptions(int provinceId) {
        return listDistrictsRaw(provinceId).stream()
                .map(district -> new GhnDistrictOption(district.getDistrictID(), district.getDistrictName(),
                        district.getProvinceID()))
                .sorted(Comparator.comparing(GhnDistrictOption::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<GhnWardOption> getWardOptions(int districtId) {
        return listWardsRaw(districtId).stream()
                .map(ward -> new GhnWardOption(ward.getWardCode(), ward.getWardName(), ward.getDistrictID()))
                .sorted(Comparator.comparing(GhnWardOption::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.provinceCache}",
        key = "'all'"
    )
    public List<GHNProvince> listProvincesRaw() {
        Map<String, Object> provincesResponse = getProvinces();
        Object data = provincesResponse.get("data");
        if (!(data instanceof List<?> rawList)) {
            throw new ShippingServiceException("SHIPPING_PROVINCES_FETCH_FAILED", "Không thể lấy danh sách tỉnh/thành từ GHN");
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> objectMapper.convertValue(item, GHNProvince.class))
                .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.districtCache}",
        key = "#provinceId"
    )
    public List<GHNDistrict> listDistrictsRaw(int provinceId) {
        Map<String, Object> districtsResponse = getDistricts(provinceId);
        Object data = districtsResponse.get("data");
        if (!(data instanceof List<?> rawList)) {
            throw new ShippingServiceException("SHIPPING_DISTRICTS_FETCH_FAILED", "Không thể lấy danh sách quận/huyện từ GHN");
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> objectMapper.convertValue(item, GHNDistrict.class))
                .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.wardCache}",
        key = "#districtId"
    )
    public List<GHNWard> listWardsRaw(int districtId) {
        Map<String, Object> wardsResponse = getWards(districtId);
        Object data = wardsResponse.get("data");
        if (!(data instanceof List<?> rawList)) {
            throw new ShippingServiceException("SHIPPING_WARDS_FETCH_FAILED", "Không thể lấy danh sách phường/xã từ GHN");
        }
        return rawList.stream()
                .filter(Objects::nonNull)
                .map(item -> objectMapper.convertValue(item, GHNWard.class))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<ShippingOption> getShippingOptions(List<CheckoutItemDetail> items, Optional<UserAddress> userAddress) {
        List<ShippingOption> result = new java.util.ArrayList<>();
        if (userAddress.isPresent()) {
            GHNWard ward;
            try {
                ward = getWard(
                        userAddress.get().getWard(),
                        userAddress.get().getProvince(),
                        userAddress.get().getDistrict());
            } catch (ShippingServiceException ex) {
                log.warn("Cannot resolve address for GHN shipping: {}", ex.getMessage());
                return List.of(ShippingOption.builder()
                        .carrier("GHN")
                        .unavailableReason(ex.getMessage())
                        .isAvailable(false)
                        .build());
            }
            log.info(ward.toString());
            if (ward.getStatus() == 2) {
                return List.of(ShippingOption.builder()
                        .carrier("GHN")
                        .unavailableReason("GHN doesn't ship to this address")
                        .isAvailable(false)
                        .build());
            }
            Map<String, Object> serviceResponse = getAllService(ward.getDistrictID(),
                    getShopDetails().getDistrict_id());
            List<Map<String, Object>> availableServices = (List<Map<String, Object>>) serviceResponse.get("data");
            for (Map<String, Object> service : availableServices) {
                if ((int) service.get("service_type_id") == 5)
                    continue;
                int shippingFee = calculateShippingFee(items, ward, String.valueOf(service.get("service_type_id")));
                int leadTime = getLeadTime(ward, String.valueOf(service.get("service_type_id")));
                ShippingOption option = ShippingOption.builder()
                        .id("GHN"+ String.valueOf(service.get("service_type_id")))
                        .serviceLevel(String.valueOf(service.get("service_type_id")))
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
        } else {
            return List.of(ShippingOption.builder()
                    .carrier("GHN")
                    .unavailableReason("No shipping address provided")
                    .isAvailable(false)
                    .build());
        }
    }

    @SuppressWarnings("unchecked")
    public int calculateShippingFee(List<CheckoutItemDetail> items, GHNWard ward, String serviceTypeId) {
        // Tính tổng trọng lượng và giá trị đơn hàng
        int totalWeight = items.stream().mapToInt(CheckoutItemDetail::getWeight).sum();
        long totalPrice = items.stream().mapToLong(CheckoutItemDetail::getTotalAmount).sum();

        double volKg = (20 * 20 * 20) / 5000.0; // kg
        int volGrams = (int) Math.ceil(volKg * 1000.0); // convert to grams and round up
        int finalWeight = Math.max(totalWeight, volGrams);

        // Tạo đối tượng CalculateFeeRequest
        CalculateFeeRequest calculateFeeRequest = CalculateFeeRequest.builder()
                .toWardCode(ward.getWardCode())
                .toDistrictId(ward.getDistrictID())
                .serviceTypeId(Integer.parseInt(serviceTypeId))
                .parcelInfor(ParcelInfor.builder()
                        .weight(finalWeight)
                        .length(20) // Chiều dài gói hàng (cm)
                        .width(20) // Chiều rộng gói hàng (cm)
                        .height(20) // Chiều cao gói hàng (cm)
                        .codAmount(totalPrice)
                        .build())
                .build();

        Map<String, Object> feeResponse = calculateFee(calculateFeeRequest);
        Map<String, Object> feeData = (Map<String, Object>) feeResponse.get("data");
        return (int) feeData.get("total");
    }

    @SuppressWarnings("unchecked")
    public int getLeadTime(GHNWard ward, String serviceTypeId) {
        GHNShopInfo shopInfo = getShopDetails();
        Map<String, Object> leadTimeResponse = getExpectedDeliveryDate(shopInfo, ward, serviceTypeId);
        Map<String, Object> data = (Map<String, Object>) leadTimeResponse.get("data");
        Integer leadTime =  (Integer) data.get("leadtime");
        if (leadTime != null) {
            return (int) Math.ceil(leadTime / 86400.0);
        }
        return 3;
    }

    private Map<String, Object> getExpectedDeliveryDate(GHNShopInfo shopInfo, GHNWard ward, String serviceTypeId) {
        log.debug("Getting expected delivery date for ward: {}", ward.getWardCode());

        try {
            Map<String, Object> requestBody = Map.of(
                    "shop_id", shopId,
                    "from_district_id", shopInfo.getDistrict_id(),
                    "from_ward_code", shopInfo.getWard_code(),
                    "to_district_id", ward.getDistrictID(),
                    "to_ward_code", ward.getWardCode(),
                    "service_id", Integer.parseInt(serviceTypeId));

            return webClientBuilder.build()
                    .post()
                    .uri(getExpectedDeliveryDateApi)
                    .header("Token", apiToken)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to get expected delivery date", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể lấy thời gian giao hàng dự kiến");
        }
    }

    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.provinceCache}",
        key = "'all'"
    )
    public Map<String, Object> getProvinces() {
        log.debug("Fetching provinces from GHN");

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(getProvinceListApi)
                    .header("Token", apiToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch provinces", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể lấy danh sách tỉnh/thành");
        }
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.districtCache}",
        key = "#provinceId"
    )
    public Map<String, Object> getDistricts(Integer provinceId) {
        log.debug("Fetching districts for province: {}", provinceId);

        try {
            return webClientBuilder.build()
                    .post()
                    .uri(getDistrictListApi)
                    .header("Token", apiToken)
                    .bodyValue(Map.of("province_id", provinceId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch districts", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể lấy danh sách quận/huyện");
        }
    }

    /**
     * Lấy danh sách phường/xã theo quận
     */
    @org.springframework.cache.annotation.Cacheable(
        value = "#{@cacheConfig.wardCache}",
        key = "#districtId"
    )
    public Map<String, Object> getWards(Integer districtId) {
        log.debug("Fetching wards for district: {}", districtId);
        Map<String, Object> body = Map.of("district_id", districtId);
        try {
            return webClientBuilder.build()
                    .post()
                    .uri(getWardListApi)
                    .header("Token", apiToken)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch wards", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể lấy danh sách phường/xã");
        }
    }

    private boolean matchesName(String input, List<String> extensions, String primaryName) {
        if (input == null || input.isBlank()) {
            return false;
        }
        String normalizedInput = normalize(input);
        if (primaryName != null && normalize(primaryName).equals(normalizedInput)) {
            return true;
        }
        if (extensions != null) {
            for (String option : extensions) {
                if (option != null && normalize(option).equals(normalizedInput)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s-]+", " ")
                .trim();
    }

    private Map<String, Object> getAllService(Integer toDistrictId, Integer fromDistrictId) {
        log.debug("Fetching all services to {}", toDistrictId);

        try {
            Map<String, Object> requestBody = Map.of(
                    "shop_id", shopId,
                    "from_district", fromDistrictId,
                    "to_district", toDistrictId);
            return webClientBuilder.build()
                    .post()
                    .uri(getServiceApi)
                    .header("Token", apiToken)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch services", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không lấy danh sách dịch vụ vận chuyển");
        }
    }

    private Map<String, Object> calculateFee(CalculateFeeRequest calculateFeeRequest) {
        log.debug("Calculating fee for request: {}", calculateFeeRequest);

        try {
            return webClientBuilder.build()
                    .post()
                    .uri(calculateFeeApi)
                    .header("Token", apiToken)
                    .header("ShopId", shopId.toString())
                    .bodyValue(calculateFeeRequest)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to calculate fee", e);
            throw new ShippingServiceException("SHIPPING_CALCULATE_FAILED", "Không thể tính phí vận chuyển");
        }
    }

    private Map<String, Object> getShopInfo() {
        log.debug("Fetching shop info from GHN");

        try {
            return webClientBuilder.build()
                    .get()
                    .uri(getShopInfoApi)
                    .header("Token", apiToken)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch shop info", e);
            throw new ShippingServiceException("SHIPPING_API_ERROR", "Không thể lấy thông tin cửa hàng");
        }
    }

    public String getPrintOrderUrl(List<String> trackingNumbers) {
        return getPrintOrderApi+getPrintToken(trackingNumbers);
    }

    public boolean cancelShippingOrder(String trackingNumber) {
        log.info("Đang gửi yêu cầu hủy đơn GHN: {}", trackingNumber);

        List<String> orderCodes = List.of(trackingNumber);

        try {
            GhnResponse<?> response = webClientBuilder.build()
                    .post()
                    .uri(cancelOrderApi)
                    .header("Token", apiToken)
                    .header("ShopId", shopId.toString())
                    .bodyValue(Map.of("order_codes", orderCodes)
                    )
                    .retrieve()
                    .bodyToMono(GhnResponse.class)
                    .block(); // Block để xử lý đồng bộ trong flow hiện tại (hoặc dùng reactive chain nếu project reactive)

            if (response != null && response.isSuccess()) {
                log.info("Hủy đơn GHN thành công: {}", trackingNumber);
                return true;
            } else {
                String msg = response != null ? response.getMessage() : "No response";
                log.error("Lỗi khi hủy đơn GHN {}: {}", trackingNumber, msg);
                // Tùy nghiệp vụ: Có thể ném lỗi hoặc return false để Admin xử lý thủ công
                throw new BadRequestException("GHN từ chối hủy đơn: " + msg);
            }
        } catch (Exception e) {
            log.error("Exception khi gọi GHN API", e);
            throw new BadRequestException("Lỗi kết nối đến GHN: " + e.getMessage());
        }
    }
}

