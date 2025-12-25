package com.example.backend.service.order;

import com.example.backend.dto.request.checkout.CheckOutItem;
import com.example.backend.dto.response.checkout.CheckoutSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutService {
    public CheckoutSession createCheckoutSession(CheckOutItem item) {
        // Implementation goes here
        return null;
    }

}
