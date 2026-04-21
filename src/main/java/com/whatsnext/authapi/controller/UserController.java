package com.whatsnext.authapi.controller;

import com.whatsnext.authapi.dto.response.UserProfileResponse;
import com.whatsnext.authapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "Authenticated user operations")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get authenticated user profile",
               security = @SecurityRequirement(name = "bearerAuth"))
    public UserProfileResponse getUser(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }
}
