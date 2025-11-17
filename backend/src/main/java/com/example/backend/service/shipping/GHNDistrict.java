package com.example.backend.service.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;

@Data
public class GHNDistrict {
    @JsonProperty("DistrictID")
    private int districtID;
    @JsonProperty("DistrictName")
    private String districtName;
    @JsonProperty("ProvinceID")
    private int provinceID;
    @JsonProperty("Status")
    private int status;
    @JsonProperty("SupportType")
    private int supportType;
}
