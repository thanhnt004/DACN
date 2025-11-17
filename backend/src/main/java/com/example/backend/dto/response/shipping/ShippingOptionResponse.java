package com.example.backend.dto.response.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShippingOptionResponse {
    private List<ShippingOption> options;

    /**
     * Phương thức đã chọn (nếu có)
     */
    private String selectedId;

    /**
     * Tùy chọn mặc định
     */
    public ShippingOption getDefault() {
        return options.stream()
                .filter(ShippingOption::getIsDefault)
                .findFirst()
                .orElse(options.isEmpty() ? null : options.get(0));
    }

    /**
     * Tìm tùy chọn theo ID
     */
    public ShippingOption findById(String id) {
        return options.stream()
                .filter(opt -> opt.getId().equals(id))
                .findFirst()
                .orElse(getDefault());
    }
}
