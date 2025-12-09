package com.example.backend.dto.request.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerRequest {
    
    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    private String title;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imageUrl;
    
    @Size(max = 500, message = "Link URL không được vượt quá 500 ký tự")
    private String linkUrl;
    
    private Boolean isActive = true;
    
    private Integer displayOrder = 0;
}
