package com.example.backend.controller.user;

import com.example.backend.dto.response.user.UserProfileDto;
import com.example.backend.dto.response.wraper.PageResponse;
import com.example.backend.model.enumrator.Role;
import com.example.backend.service.user.UserManagerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {
    private final UserManagerService userManagerService;
    @GetMapping
    @Operation(
            summary = "Get list of all users with filters and sorting"
    )
    public ResponseEntity<?> getUserList(@RequestParam(value = "role",required = false) Role role,
                                         @RequestParam(value = "isActive", required = false) Boolean isActive,
                                         @PageableDefault(size = 20, page = 0)Pageable page
                                         ){
        PageResponse<UserProfileDto> response = userManagerService.getUserList(
                role,
                isActive,
                page
        );
        return ResponseEntity.ok(response);
    }
    @PostMapping("/ban/{userId}")
    @Operation(
            summary = "Delete an user"
    )
    public ResponseEntity<?> ban(@PathVariable UUID userId){
        userManagerService.band(userId);
        return ResponseEntity.ok().body(Map.of("status", "success"));
    }

    @PostMapping("/restore_user/{userId}")
    @Operation(
            summary = "Restore an user"
    )
    public ResponseEntity<?> restoreUser(@PathVariable UUID userId){
        userManagerService.restoreUser(userId);
        return ResponseEntity.ok().body(Map.of("status", "success"));
    }
}
