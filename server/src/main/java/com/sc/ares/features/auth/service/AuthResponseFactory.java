package com.sc.ares.features.auth.service;

import com.sc.ares.common.security.AresAuthenticatedPrincipal;
import com.sc.ares.common.security.AresUserDetails;
import com.sc.ares.features.auth.dto.AuthenticatedUserResponse;
import java.util.List;
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
            principal.companyId(),
            userDetails.getCompanyName(),
            List.copyOf(userDetails.getRoleCodes()),
            principal.platformAdmin(),
            "BASIC"
        );
    }
}
