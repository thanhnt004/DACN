package com.example.backend.service.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GHNWard {
    @JsonProperty("WardCode")
    private String wardCode;
    @JsonProperty("WardName")
    private String wardName;
    @JsonProperty("DistrictID")
    private int districtID;
    @JsonProperty("SupportType")
    private int supportType;
    @JsonProperty("Status")
    private int status;
}
