package com.example.backend.dto.response.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class UserAddress {
    private UUID id;
    private String fullName;
    private String phone;
    private String line1;
    private String line2;
    private String ward;
    private String district;
    private String province;
    @JsonProperty("isDefaultShipping")
    private boolean isDefaultShipping = false;

    @JsonIgnore
    public String getAddressLine() {
        StringBuilder sb = new StringBuilder();

        if (line1 != null && !line1.isBlank()) sb.append(line1);
        if (line2 != null && !line2.isBlank()) sb.append(", ").append(line2);
        if (ward != null && !ward.isBlank()) sb.append(", ").append(ward);
        if (district != null && !district.isBlank()) sb.append(", ").append(district);
        if (province != null && !province.isBlank()) sb.append(", ").append(province);

        return sb.toString();
    }
}
