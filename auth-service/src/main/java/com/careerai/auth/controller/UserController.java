package com.careerai.auth.controller;

import com.careerai.auth.domain.entity.User;
import com.careerai.auth.dto.request.UpdateProfileRequest;
import com.careerai.auth.dto.response.UserResponse;
import com.careerai.auth.service.AuthService;
import com.careerai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated endpoints for the current user's profile.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current-user profile operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AuthService authService;

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(principal.getEmail())));
    }

    @Operation(summary = "Update the authenticated user's profile")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal User principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Profile updated", authService.updateProfile(principal.getEmail(), request)));
    }
}
