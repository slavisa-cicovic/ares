package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.IssuedAccessToken;
import com.sc.ares.common.security.AresUserDetails;
import com.sc.ares.features.auth.dto.AuthTokenResponse;
import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthResponseFactory {

    public AuthenticatedUserResponse fromUserDetails(AresUserDetails userDetails) {
        AresAuthenticatedPrincipal principal = userDetails.getPrincipal();

        return new AuthenticatedUserResponse(
            principal.userId(),
            principal.email(),
            userDetails.getFirstName(),
            userDetails.getLastName(),
            userDetails.getPhone(),
            userDetails.getProfileImageUrl(),
            principal.companyId(),
            userDetails.getCompanyName(),
            userDetails.getCompanyBrandColor(),
            userDetails.getCompanyLogoUrl(),
            userDetails.getCompanyThemePreset(),
            userDetails.getRoleCodes(),
            principal.platformAdmin()
        );
    }

    public AuthTokenResponse tokenResponse(
        AresUserDetails userDetails,
        IssuedAccessToken accessToken,
        IssuedRefreshToken refreshToken
    ) {
        return new AuthTokenResponse(
            accessToken.token(),
            refreshToken.token(),
            accessToken.expiresAt().toString(),
            refreshToken.expiresAt().toString(),
            fromUserDetails(userDetails)
        );
    }
}
