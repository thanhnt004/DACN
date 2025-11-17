package com.example.backend.service.shipping;

import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.service.user.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingService {
    private final GHNService ghnService;
    private final UserManagerService userManagerService;
    public List<ShippingOption> getAvailableShippingOptions(Optional<UserAddress> userAddressOps, List<CheckoutItemDetail> items) {
        return ghnService.getShippingOptions(items, userAddressOps);
    }

}
