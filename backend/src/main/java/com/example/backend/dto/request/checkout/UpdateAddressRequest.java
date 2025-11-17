package com.example.backend.dto.request.checkout;

import com.example.backend.dto.response.user.UserAddress;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddressRequest {
    @NotNull
    @JsonUnwrapped
    private UserAddress userAddress;
}
