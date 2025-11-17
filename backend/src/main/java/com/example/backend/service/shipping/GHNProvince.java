package com.example.backend.service.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import jdk.dynalink.linker.LinkerServices;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class GHNProvince {
    @JsonProperty("ProvinceID")
    private int provinceID;

    @JsonProperty("ProvinceName")
    private String provinceName;

    @JsonProperty("Status")
    private int status;
}