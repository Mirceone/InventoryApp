package com.mirceone.inventoryapp.api.auth;

import com.mirceone.inventoryapp.service.auth.AuthContracts;

public final class AuthWebMapper {

    private AuthWebMapper() {
    }

    public static AuthContracts.SignupSpec toSignupSpec(SignupRequest request) {
        return new AuthContracts.SignupSpec(request.email(), request.password(), request.displayName());
    }

    public static AuthContracts.LoginSpec toLoginSpec(LoginRequest request) {
        return new AuthContracts.LoginSpec(request.email(), request.password());
    }

    public static AuthContracts.RefreshSpec toRefreshSpec(RefreshRequest request) {
        return new AuthContracts.RefreshSpec(request.refreshToken());
    }

    public static AuthContracts.LogoutSpec toLogoutSpec(LogoutRequest request) {
        return new AuthContracts.LogoutSpec(request.refreshToken());
    }

    public static AuthContracts.ForgotPasswordSpec toForgotPasswordSpec(ForgotPasswordRequest request) {
        return new AuthContracts.ForgotPasswordSpec(request.email());
    }

    public static AuthContracts.CompletePasswordResetSpec toCompletePasswordResetSpec(ResetPasswordRequest request) {
        return new AuthContracts.CompletePasswordResetSpec(request.token(), request.newPassword());
    }

    public static AuthResponse toAuthResponse(AuthContracts.IssuedTokenPair tokens) {
        return new AuthResponse(
                tokens.tokenType(),
                tokens.accessToken(),
                tokens.expiresInSeconds(),
                tokens.refreshToken(),
                tokens.refreshExpiresInSeconds()
        );
    }

    public static MeResponse toMeResponse(AuthContracts.CurrentUserSnapshot user) {
        return new MeResponse(user.id(), user.email(), user.displayName(), user.provider());
    }
}
