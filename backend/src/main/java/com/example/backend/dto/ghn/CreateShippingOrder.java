package com.example.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShippingOrder {
    @JsonProperty("payment_type_id") int paymentTypeId;
    @JsonProperty("note") String note;
    @JsonProperty("required_note") String requiredNote;
    @JsonProperty("client_order_code") String clientOrderCode;

    // Receiver address (người nhận)
    @JsonProperty("to_name") String toName;
    @JsonProperty("to_phone") String toPhone;
    @JsonProperty("to_address") String toAddress;
    @JsonProperty("to_ward_code") String toWardCode;
    @JsonProperty("to_district_id") Integer toDistrictId;
    @JsonProperty("to_ward_name") String toWardName;
    @JsonProperty("to_district_name") String toDistrictName;
    @JsonProperty("to_province_name") String toProvinceName;
    
    @JsonProperty("content") String content;
    
    // Sender address (người gửi) - CHỈ DÙNG TEXT NAME
    @JsonProperty("from_name") String fromName;
    @JsonProperty("from_phone") String fromPhone;
    @JsonProperty("from_address") String fromAddress;
    @JsonProperty("from_ward_name") String fromWardName;
    @JsonProperty("from_district_name") String fromDistrictName;
    @JsonProperty("from_province_name") String fromProvinceName;

    @JsonProperty("return_phone")
    private String returnPhone;
    @JsonProperty("return_address")
    private String returnAddress;
    @JsonProperty("pick_station_id")
    private Integer pickStationId;

    @JsonUnwrapped
    private ParcelInfor parcelInfor;
    @JsonProperty("service_type_id") int serviceTypeId;
    @JsonProperty("items")
    List<GHNItem> items;

}
