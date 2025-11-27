package com.example.backend.dto.request.order;

import com.example.backend.dto.response.user.UserAddress;
import lombok.Data;

@Data
public class ReturnOrderRequest {
    private UserAddress returnAddress;
    private ReturnOption returnOption;
    private String reason;
}
