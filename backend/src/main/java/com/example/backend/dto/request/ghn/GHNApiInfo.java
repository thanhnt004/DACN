package com.example.backend.dto.request.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GHNApiInfo {
    @Value("${ghn.api.token}")
    private String token;
    @Value("${ghn.shop.id}")
    private String shopId;
}
