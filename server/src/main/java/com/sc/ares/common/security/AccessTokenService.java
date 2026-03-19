package com.sc.ares.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sc.ares.config.AuthTokenProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final AuthTokenProperties authTokenProperties;
    private final ObjectMapper objectMapper;

    public AccessTokenService(
        AuthTokenProperties authTokenProperties,
        ObjectMapper objectMapper
    ) {
        this.authTokenProperties = authTokenProperties;
        this.objectMapper = objectMapper;
    }

    public IssuedAccessToken issue(AresUserDetails userDetails) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authTokenProperties.getAccessTokenTtl());
        UUID tokenId = UUID.randomUUID();

        Map<String, Object> header = Map.of(
            "alg", "HS256",
            "typ", "JWT"
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", authTokenProperties.getIssuer());
        payload.put("jti", tokenId.toString());
        payload.put("sub", userDetails.getPrincipal().userId().toString());
        payload.put("companyId", userDetails.getPrincipal().companyId().toString());
        payload.put("email", userDetails.getPrincipal().email());
        payload.put("platformAdmin", userDetails.getPrincipal().platformAdmin());
        payload.put("roles", userDetails.getRoleCodes());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());
        payload.put("type", "access");

        String token = signToken(header, payload);

        return new IssuedAccessToken(token, expiresAt);
    }

    public AccessTokenClaims parse(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new BadCredentialsException("Invalid access token.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);

        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BadCredentialsException("Invalid access token.");
        }

        Map<String, Object> payload = readJson(base64UrlDecode(parts[1]));

        if (!"access".equals(payload.get("type"))) {
            throw new BadCredentialsException("Invalid access token.");
        }

        long expiresAtEpoch = getLong(payload, "exp");
        Instant expiresAt = Instant.ofEpochSecond(expiresAtEpoch);

        if (expiresAt.isBefore(Instant.now())) {
            throw new BadCredentialsException("Expired access token.");
        }

        return new AccessTokenClaims(
            UUID.fromString(getString(payload, "jti")),
            UUID.fromString(getString(payload, "sub")),
            UUID.fromString(getString(payload, "companyId")),
            getString(payload, "email"),
            getBoolean(payload, "platformAdmin"),
            getStringList(payload, "roles"),
            Instant.ofEpochSecond(getLong(payload, "iat")),
            expiresAt
        );
    }

    private String signToken(Map<String, Object> header, Map<String, Object> payload) {
        try {
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;

            return unsignedToken + "." + sign(unsignedToken);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize access token.", exception);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                authTokenProperties.getAccessTokenSecret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));
            return base64UrlEncode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign access token.", exception);
        }
    }

    private Map<String, Object> readJson(byte[] content) {
        try {
            return objectMapper.readValue(content, MAP_TYPE);
        } catch (IOException exception) {
            throw new BadCredentialsException("Invalid access token.");
        }
    }

    private byte[] base64UrlDecode(String value) {
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("Invalid access token.");
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String getString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new BadCredentialsException("Invalid access token.");
        }

        return stringValue;
    }

    private boolean getBoolean(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (!(value instanceof Boolean booleanValue)) {
            throw new BadCredentialsException("Invalid access token.");
        }

        return booleanValue;
    }

    private long getLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new BadCredentialsException("Invalid access token.");
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);

        if (!(value instanceof List<?> listValue)) {
            throw new BadCredentialsException("Invalid access token.");
        }

        if (!listValue.stream().allMatch(String.class::isInstance)) {
            throw new BadCredentialsException("Invalid access token.");
        }

        return (List<String>) listValue;
    }
}
