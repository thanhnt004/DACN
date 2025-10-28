package com.example.backend.controller.user;

import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.dto.response.user.OauthProviderResponse;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.dto.response.user.UserProfileDto;
import com.example.backend.service.auth.UserDetailService;
import com.example.backend.service.user.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/me")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    private final UserManagerService userManagerService;

    @GetMapping
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<UserProfileDto> getUserProfile(@AuthenticationPrincipal CustomUserDetail userDetail)
    {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();

        log.info("auth: {}, authenticated: {}, class: {}", a, a != null ? a.isAuthenticated() : null, a != null ? a.getClass() : null);
        return ResponseEntity.ok(userManagerService.getUserProfile(userDetail));
    }
    @GetMapping(value = "/address")
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<List<UserAddress>> getAddresses(@AuthenticationPrincipal CustomUserDetail userDetail)
    {
        return ResponseEntity.ok(userManagerService.getAddressList(userDetail));
    }
    @GetMapping(value = "/linked-account")
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<List<OauthProviderResponse>> getLinkedProvider(@AuthenticationPrincipal CustomUserDetail userDetail)
    {
        return ResponseEntity.ok(userManagerService.getLinkedProvider(userDetail));
    }
    @PostMapping(value = "/address")
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<?> addNewAddress(@AuthenticationPrincipal CustomUserDetail userDetail,@RequestBody UserAddress address)
    {
        userManagerService.addAddress(userDetail,address);
        return ResponseEntity.noContent().build();
    }
    @PutMapping
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal CustomUserDetail userDetail,@RequestBody UserProfileDto dto)
    {
        userManagerService.updateProfile(userDetail,dto);
        return ResponseEntity.noContent().build();
    }
    @PutMapping(value = "/addresses/{addressId}")
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<?> updateAddress( @RequestBody UserAddress dto, @PathVariable("addressId")UUID addressId)
    {
        userManagerService.updateAddress(dto,addressId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping(value = "/addresses/{addressId}")
    @PreAuthorize(value = "hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteAddress(@PathVariable("addressId")UUID addressId)
    {
        userManagerService.deleteAddress(addressId);
        return ResponseEntity.noContent().build();
    }
}
