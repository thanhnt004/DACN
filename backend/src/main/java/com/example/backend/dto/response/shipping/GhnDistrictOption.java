package com.example.backend.dto.response.shipping;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GhnDistrictOption {
    private final int id;
    private final String name;
    private final int provinceId;
}
