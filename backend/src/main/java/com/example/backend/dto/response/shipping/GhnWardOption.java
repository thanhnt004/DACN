package com.example.backend.dto.response.shipping;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GhnWardOption {
    private final String code;
    private final String name;
    private final int districtId;
}
