package com.mirceone.inventoryapp.api.auth;

import com.mirceone.inventoryapp.api.support.CurrentUserId;
import com.mirceone.inventoryapp.service.auth.AuthService;
import com.mirceone.inventoryapp.service.auth.PasswordResetService;
import com.mirceone.inventoryapp.service.firms.members.FirmInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Authentication and session endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final FirmInvitationService firmInvitationService;

    public AuthController(
            AuthService authService,
            PasswordResetService passwordResetService,
            FirmInvitationService firmInvitationService
    ) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.firmInvitationService = firmInvitationService;
    }

    @PostMapping("/signup")
    @Operation(summary = "Create local user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signup successful"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return AuthWebMapper.toAuthResponse(authService.signup(AuthWebMapper.toSignupSpec(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid provider for password login"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthWebMapper.toAuthResponse(authService.login(AuthWebMapper.toLoginSpec(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refresh successful"),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or revoked")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return AuthWebMapper.toAuthResponse(authService.refresh(AuthWebMapper.toRefreshSpec(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token (logout)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(AuthWebMapper.toLogoutSpec(request));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke all active sessions for current user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All sessions revoked"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    public void logoutAll(@CurrentUserId UUID userId) {
        authService.logoutAll(userId);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request password reset link by email")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Request accepted (no email enumeration)"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(AuthWebMapper.toForgotPasswordSpec(request));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set new password using reset token from email link")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired reset token")
    })
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(AuthWebMapper.toCompletePasswordResetSpec(request));
    }

    @GetMapping("/invitations/{token}")
    @Operation(summary = "Preview firm invitation from email link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation preview returned"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired invitation token")
    })
    public InvitationPreviewResponse previewInvitation(@PathVariable String token) {
        return AuthWebMapper.toInvitationPreviewResponse(firmInvitationService.previewInvitation(token));
    }

    @PostMapping("/accept-invitation")
    @Operation(summary = "Accept firm invitation (new account or logged-in user)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted, tokens issued"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Login required or invalid token"),
            @ApiResponse(responseCode = "403", description = "Logged-in user does not match invitation email"),
            @ApiResponse(responseCode = "409", description = "Already a firm member")
    })
    public AuthResponse acceptInvitation(
            @CurrentUserId(required = false) UUID userId,
            @Valid @RequestBody AcceptInvitationRequest request
    ) {
        return AuthWebMapper.toAuthResponse(
                firmInvitationService.acceptInvitation(AuthWebMapper.toAcceptInvitationSpec(request), userId)
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token")
    })
    public MeResponse me(@CurrentUserId UUID userId) {
        return AuthWebMapper.toMeResponse(authService.getMe(userId));
    }
}
