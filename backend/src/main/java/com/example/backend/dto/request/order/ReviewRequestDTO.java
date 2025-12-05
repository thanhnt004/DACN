package com.example.backend.dto.request.order;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDTO {
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Trạng thái không hợp lệ")
    private String status;
    private String adminNote;
}
