package com.example.backend.controller.shipping;

import com.example.backend.dto.response.shipping.GhnDistrictOption;
import com.example.backend.dto.response.shipping.GhnProvinceOption;
import com.example.backend.dto.response.shipping.GhnWardOption;
import com.example.backend.service.shipping.GHNService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipping/ghn")
@RequiredArgsConstructor
public class ShippingLocationController {
    private final GHNService ghnService;

    @GetMapping("/provinces")
    public ResponseEntity<List<GhnProvinceOption>> getProvinces() {
        return ResponseEntity.ok(ghnService.getProvinceOptions());
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<List<GhnDistrictOption>> getDistricts(@PathVariable int provinceId) {
        return ResponseEntity.ok(ghnService.getDistrictOptions(provinceId));
    }

    @GetMapping("/districts/{districtId}/wards")
    public ResponseEntity<List<GhnWardOption>> getWards(@PathVariable int districtId) {
        return ResponseEntity.ok(ghnService.getWardOptions(districtId));
    }
}
