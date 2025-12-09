package com.example.backend.dto.request.banner;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerReorderRequest {
    
    @NotNull(message = "Danh sách banner không được để trống")
    private List<BannerOrderItem> banners;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BannerOrderItem {
        @NotNull(message = "ID banner không được để trống")
        private UUID id;
        
        @NotNull(message = "Thứ tự hiển thị không được để trống")
        private Integer displayOrder;
    }
}
