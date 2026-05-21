package com._202510007517.platform.gateway.security;

import com._202510007517.platform.gateway.config.GatewaySecurityProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class JwtTokenValidator {

    private final GatewaySecurityProperties securityProperties;
    private final Optional<ReactiveStringRedisTemplate> redisTemplate;
    private final Clock clock;

    @Autowired
    public JwtTokenValidator(GatewaySecurityProperties securityProperties,
                             Optional<ReactiveStringRedisTemplate> redisTemplate) {
        this(securityProperties, redisTemplate, Clock.systemUTC());
    }

    JwtTokenValidator(GatewaySecurityProperties securityProperties,
                      Optional<ReactiveStringRedisTemplate> redisTemplate,
                      Clock clock) {
        this.securityProperties = securityProperties;
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public Mono<GatewayJwtAuthContext> validate(String rawToken) {
        SignedJWT signedJwt = parse(rawToken);
        String kid = signedJwt.getHeader().getKeyID();
        RSAPublicKey publicKey = resolvePublicKey(kid);
        verifySignature(signedJwt, publicKey);

        JWTClaimsSet claims = getClaims(signedJwt);
        validateExpiry(claims);

        GatewayJwtAuthContext authContext = buildAuthContext(claims);
        return ensureNotBlacklisted(authContext);
    }

    private SignedJWT parse(String rawToken) {
        try {
            return SignedJWT.parse(rawToken);
        } catch (ParseException ex) {
            throw new JwtAuthenticationException("JWT 格式不合法");
        }
    }

    private RSAPublicKey resolvePublicKey(String kid) {
        if (kid == null || kid.isBlank()) {
            throw new JwtAuthenticationException("JWT 缺少 kid");
        }
        Map<String, String> publicKeys = securityProperties.getJwt().getPublicKeys();
        String publicKeyText = publicKeys.get(kid);
        if (publicKeyText == null || publicKeyText.isBlank()) {
            throw new JwtAuthenticationException("JWT kid 未受信任");
        }
        return RsaPublicKeyParser.parse(publicKeyText);
    }

    private void verifySignature(SignedJWT signedJwt, RSAPublicKey publicKey) {
        try {
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())
                    && !JWSAlgorithm.RS512.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new JwtAuthenticationException("JWT 签名算法不受支持");
            }
            RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJwt.verify(verifier)) {
                throw new JwtAuthenticationException("JWT 签名校验失败");
            }
        } catch (JOSEException ex) {
            throw new JwtAuthenticationException("JWT 签名校验失败");
        }
    }

    private JWTClaimsSet getClaims(SignedJWT signedJwt) {
        try {
            return signedJwt.getJWTClaimsSet();
        } catch (ParseException ex) {
            throw new JwtAuthenticationException("JWT claims 解析失败");
        }
    }

    private void validateExpiry(JWTClaimsSet claims) {
        Date expiresAt = claims.getExpirationTime();
        if (expiresAt == null) {
            throw new JwtAuthenticationException("JWT 缺少过期时间");
        }
        Instant now = clock.instant();
        Instant deadline = expiresAt.toInstant().plusSeconds(securityProperties.getJwt().getClockSkewSeconds());
        if (deadline.isBefore(now)) {
            throw new JwtAuthenticationException("JWT 已过期");
        }
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && notBefore.toInstant().isAfter(now.plusSeconds(securityProperties.getJwt().getClockSkewSeconds()))) {
            throw new JwtAuthenticationException("JWT 尚未生效");
        }
    }

    private GatewayJwtAuthContext buildAuthContext(JWTClaimsSet claims) {
        String userId = resolveUserId(claims);
        List<String> roles = resolveRoles(claims.getClaim("roles"));
        if (roles.isEmpty()) {
            throw new JwtAuthenticationException("JWT 缺少角色信息");
        }
        String activeRole = normalizeRole(Objects.toString(claims.getClaim("activeRole"), null));
        if (activeRole == null) {
            activeRole = roles.get(0);
        } else if (!roles.contains(activeRole)) {
            throw new JwtAuthenticationException("JWT activeRole 非法");
        }
        return new GatewayJwtAuthContext(userId, roles, activeRole, claims.getJWTID(), claims.getSubject());
    }

    private String resolveUserId(JWTClaimsSet claims) {
        Object claimValue = claims.getClaim("userId");
        if (claimValue != null && !claimValue.toString().isBlank()) {
            return claimValue.toString().trim();
        }
        if (claims.getSubject() != null && !claims.getSubject().isBlank()) {
            return claims.getSubject().trim();
        }
        throw new JwtAuthenticationException("JWT 缺少用户标识");
    }

    private List<String> resolveRoles(Object rawRoles) {
        LinkedHashSet<String> roleSet = new LinkedHashSet<>();
        if (rawRoles instanceof Collection<?> collection) {
            collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(this::normalizeRole)
                    .filter(Objects::nonNull)
                    .forEach(roleSet::add);
        } else if (rawRoles instanceof String stringValue) {
            for (String item : stringValue.split(",")) {
                String normalized = normalizeRole(item);
                if (normalized != null) {
                    roleSet.add(normalized);
                }
            }
        }
        return new ArrayList<>(roleSet);
    }

    private String normalizeRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Mono<GatewayJwtAuthContext> ensureNotBlacklisted(GatewayJwtAuthContext authContext) {
        if (authContext.jti() == null || authContext.jti().isBlank() || redisTemplate.isEmpty()) {
            return Mono.just(authContext);
        }
        String key = securityProperties.getJwt().getJtiBlacklistPrefix() + authContext.jti();
        return redisTemplate.get().hasKey(key)
                .flatMap(blacklisted -> blacklisted
                        ? Mono.error(new JwtAuthenticationException("JWT 已失效"))
                        : Mono.just(authContext));
    }
}
