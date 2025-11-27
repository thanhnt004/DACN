package com.example.backend.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CalculateFeeRequest {
    @JsonProperty("service_type_id")
    private int serviceTypeId;

    @JsonProperty("to_district_id")
    private int toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;
    @JsonUnwrapped
    private ParcelInfor parcelInfor;
}
