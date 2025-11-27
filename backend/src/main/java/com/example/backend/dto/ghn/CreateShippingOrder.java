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
    @JsonProperty("shop_id") int shopId;
    @JsonProperty("payment_type_id") int paymentTypeId;
    @JsonProperty("required_note") String requiredNote;
    @JsonProperty("to_name") String toName;
    @JsonProperty("to_phone") String toPhone;
    @JsonProperty("to_address") String toAddress;
    @JsonProperty("to_ward_code") String toWardCode;
    @JsonProperty("to_district_id") int toDistrictId;
    @JsonProperty("content") String content;
    @JsonUnwrapped
    private ParcelInfor parcelInfor;
    @JsonProperty("service_type_id") int serviceTypeId;
    @JsonProperty("items")
    List<GHNItem> items;

////    //sender info
////    @JsonProperty("from_name")
////    private String fromName;
////    @JsonProperty("from_phone")
////    private String fromPhone;
////    @JsonProperty("from_address")
////    private String fromAddress;
////    @JsonProperty("from_ward_code")
////    private String fromWardName;
////    @JsonProperty("from_district_id")
////    private String fromDistrictName;
////    @JsonProperty("from_province_id")
////    private String fromProvinceName;
}
