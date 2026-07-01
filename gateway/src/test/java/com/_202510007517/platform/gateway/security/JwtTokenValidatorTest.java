package com._202510007517.platform.gateway.security;

import com._202510007517.platform.common.security.DevJwtKeyMaterial;
import com._202510007517.platform.gateway.config.GatewaySecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenValidatorTest {

    @Test
    void validatesDevelopmentJwtWhenNoPublicKeyIsConfigured() throws JOSEException {
        JwtTokenValidator validator = new JwtTokenValidator(
                new GatewaySecurityProperties(),
                Optional.empty(),
                Clock.fixed(Instant.parse("2026-06-07T10:00:00Z"), ZoneOffset.UTC));
        String token = signDevelopmentToken();

        GatewayJwtAuthContext context = validator.validate(token).block();

        assertThat(context).isNotNull();
        assertThat(context.userId()).isEqualTo("42");
        assertThat(context.roles()).containsExactly("STUDENT");
        assertThat(context.activeRole()).isEqualTo("STUDENT");
    }

    private static String signDevelopmentToken() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("42")
                .claim("userId", 42L)
                .claim("roles", List.of("STUDENT"))
                .claim("activeRole", "STUDENT")
                .issueTime(Date.from(Instant.parse("2026-06-07T09:59:00Z")))
                .expirationTime(Date.from(Instant.parse("2026-06-07T10:30:00Z")))
                .jwtID("dev-jti")
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(DevJwtKeyMaterial.KID).build(),
                claims);
        jwt.sign(new RSASSASigner(DevJwtKeyMaterial.privateKey()));
        return jwt.serialize();
    }
}
