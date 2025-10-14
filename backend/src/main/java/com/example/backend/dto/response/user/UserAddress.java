package com.example.backend.dto.response.user;

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
    private String city;
    private boolean isDefaultShipping = false;
}
