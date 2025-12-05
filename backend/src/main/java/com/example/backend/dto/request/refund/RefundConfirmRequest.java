package com.example.backend.dto.request.refund;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundConfirmRequest {
    private String imageProof;
    private String note;
}
