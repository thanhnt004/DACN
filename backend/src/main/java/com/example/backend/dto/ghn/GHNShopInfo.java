package com.example.backend.dto.ghn;

import lombok.Data;

@Data
public class GHNShopInfo {
    private long _id;
    private String name;
    private String phone;
    private String address;
    private String ward_code;
    private int district_id;
    private int province_id;
    private long client_id;
    private int status;
}
