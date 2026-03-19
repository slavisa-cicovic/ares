package com.sc.ares.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTH_ERROR_MESSAGE_ATTRIBUTE = "ares.auth.error.message";

    private final AccessTokenService accessTokenService;
    private final AresAuthenticationEntryPoint authenticationEntryPoint;

    public AccessTokenAuthenticationFilter(
        AccessTokenService accessTokenService,
        AresAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.accessTokenService = accessTokenService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            commenceUnauthorized(request, response, "Invalid access token.");
            return;
        }

        try {
            AccessTokenClaims claims = accessTokenService.parse(token);
            AresAuthenticatedPrincipal principal = new AresAuthenticatedPrincipal(
                claims.userId(),
                claims.companyId(),
                claims.email(),
                claims.platformAdmin(),
                claims.roleCodes().stream()
                    .map(this::toAuthorityCode)
                    .map(SimpleGrantedAuthority::new)
                    .toList()
            );

            UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    token,
                    principal.authorities()
                );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            commenceUnauthorized(request, response, "Invalid access token.");
        }
    }

    private void commenceUnauthorized(
        HttpServletRequest request,
        HttpServletResponse response,
        String message
    ) throws IOException, ServletException {
        request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, message);
        authenticationEntryPoint.commence(
            request,
            response,
            new BadCredentialsException(message)
        );
    }

    private String toAuthorityCode(String roleCode) {
        return "ROLE_" + roleCode.toUpperCase().replace('-', '_');
    }
}
