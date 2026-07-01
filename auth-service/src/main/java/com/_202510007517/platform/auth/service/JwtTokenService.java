package com._202510007517.platform.auth.service;

import com._202510007517.platform.auth.config.AuthProperties;
import com._202510007517.platform.common.exception.UnauthorizedException;
import com._202510007517.platform.common.security.DevJwtKeyMaterial;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtTokenService {

    private final AuthProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, RSAPublicKey> publicKeyCache = new ConcurrentHashMap<>();
    private final Map<String, RSAPrivateKey> privateKeyCache = new ConcurrentHashMap<>();

    public JwtTokenService(AuthProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public TokenPair issueTokenPair(Long userId, String username, List<String> roles, String activeRole) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        String jti = UUID.randomUUID().toString();
        String accessToken = sign(userId, username, roles, activeRole, "access", jti, now, expiresAt);
        rememberUserToken(userId, jti);
        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(refreshKey(refreshToken), userId + ":" + activeRole, properties.getRefreshTokenTtl());
        return new TokenPair(accessToken, refreshToken, "Bearer", expiresAt);
    }

    public AuthTokenClaims verifyAccessToken(String token) {
        SignedJWT signedJwt = parse(token);
        JWTClaimsSet claims = verifyAndReadClaims(signedJwt);
        if (!"access".equals(stringClaim(claims, "tokenType"))) {
            throw new UnauthorizedException("Token 类型不正确");
        }
        String jti = claims.getJWTID();
        if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey(jti)))) {
            throw new UnauthorizedException("Token 已失效");
        }
        return toClaims(claims);
    }

    public AuthTokenClaims verifyIgnoringBlacklist(String token) {
        return toClaims(verifyAndReadClaims(parse(token)));
    }

    public void revokeAccessToken(String token) {
        AuthTokenClaims claims = verifyIgnoringBlacklist(token);
        if (claims.jti() == null || claims.expiresAt() == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().setIfAbsent(blacklistKey(claims.jti()), "1", ttl);
        }
    }

    public TokenPair refresh(String refreshToken, String username, List<String> roles) {
        RefreshContext context = readRefreshContext(refreshToken);
        TokenPair tokenPair = issueTokenPair(context.userId(), username, roles, context.activeRole());
        deleteRefreshToken(refreshToken);
        return tokenPair;
    }

    public RefreshContext readRefreshContext(String refreshToken) {
        String value = redisTemplate.opsForValue().get(refreshKey(refreshToken));
        if (value == null || value.isBlank()) {
            throw new UnauthorizedException("Refresh Token 无效");
        }
        String[] parts = value.split(":", 2);
        Long userId = Long.valueOf(parts[0]);
        String activeRole = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "STUDENT";
        return new RefreshContext(userId, activeRole);
    }

    public void deleteRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(refreshKey(refreshToken));
        }
    }

    public void revokeByUser(Long userId) {
        String key = userTokenKey(userId);
        var members = redisTemplate.opsForSet().members(key);
        if (members != null) {
            for (String jti : members) {
                if (jti != null && !jti.isBlank()) {
                    redisTemplate.opsForValue().setIfAbsent(blacklistKey(jti), "1", properties.getAccessTokenTtl());
                }
            }
        }
        redisTemplate.delete(key);
    }

    public String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少 Bearer Token");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Bearer Token 不能为空");
        }
        return token;
    }

    private String sign(Long userId, String username, List<String> roles, String activeRole,
                        String tokenType, String jti, Instant issuedAt, Instant expiresAt) {
        try {
            String kid = properties.getActiveKid();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("userId", userId)
                    .claim("username", username)
                    .claim("roles", roles)
                    .claim("activeRole", normalizeRole(activeRole))
                    .claim("tokenType", tokenType)
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .jwtID(jti)
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(kid).build(), claims);
            signedJWT.sign(new RSASSASigner(resolvePrivateKey(kid)));
            return signedJWT.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("JWT 签发失败", ex);
        }
    }

    private JWTClaimsSet verifyAndReadClaims(SignedJWT signedJwt) {
        try {
            String kid = signedJwt.getHeader().getKeyID();
            if (kid == null || kid.isBlank()) {
                throw new UnauthorizedException("Token 缺少 kid");
            }
            if (!signedJwt.verify(new RSASSAVerifier(resolvePublicKey(kid)))) {
                throw new UnauthorizedException("Token 签名校验失败");
            }
            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            Date expiresAt = claims.getExpirationTime();
            if (expiresAt == null || expiresAt.toInstant().isBefore(Instant.now())) {
                throw new UnauthorizedException("Token 已过期");
            }
            return claims;
        } catch (JOSEException | ParseException ex) {
            throw new UnauthorizedException("Token 校验失败");
        }
    }

    private SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException ex) {
            throw new UnauthorizedException("Token 格式不正确");
        }
    }

    private AuthTokenClaims toClaims(JWTClaimsSet claims) {
        try {
            Object userIdClaim = claims.getClaim("userId");
            Long userId = userIdClaim instanceof Number number
                    ? number.longValue()
                    : Long.valueOf(String.valueOf(userIdClaim));
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.getClaim("roles");
            return new AuthTokenClaims(
                    userId,
                    claims.getSubject(),
                    roles,
                    stringClaim(claims, "activeRole"),
                    claims.getJWTID(),
                    stringClaim(claims, "tokenType"),
                    claims.getExpirationTime().toInstant()
            );
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Token claims 不完整");
        }
    }

    private RSAPublicKey resolvePublicKey(String kid) {
        if (properties.getKeys().isEmpty()) {
            return DevJwtKeyMaterial.publicKey();
        }
        return publicKeyCache.computeIfAbsent(kid, key -> {
            AuthProperties.KeyPairProperties configured = properties.getKeys().get(key);
            if (configured == null || configured.getPublicKey() == null) {
                throw new UnauthorizedException("Token kid 未受信任");
            }
            return RsaKeySupport.parsePublicKey(configured.getPublicKey());
        });
    }

    private RSAPrivateKey resolvePrivateKey(String kid) {
        if (properties.getKeys().isEmpty()) {
            return DevJwtKeyMaterial.privateKey();
        }
        return privateKeyCache.computeIfAbsent(kid, key -> {
            AuthProperties.KeyPairProperties configured = properties.getKeys().get(key);
            if (configured == null || configured.getPrivateKey() == null) {
                throw new IllegalStateException("当前 kid 未配置私钥: " + key);
            }
            return RsaKeySupport.parsePrivateKey(configured.getPrivateKey());
        });
    }

    private String normalizeRole(String role) {
        return role == null ? null : role.trim().toUpperCase(Locale.ROOT);
    }

    private String stringClaim(JWTClaimsSet claims, String claimName) {
        try {
            return claims.getStringClaim(claimName);
        } catch (ParseException ex) {
            throw new UnauthorizedException("Token claim 类型不正确: " + claimName);
        }
    }

    private String blacklistKey(String jti) {
        return properties.getJtiBlacklistPrefix() + jti;
    }

    private String refreshKey(String refreshToken) {
        return properties.getRefreshTokenPrefix() + refreshToken;
    }

    private void rememberUserToken(Long userId, String jti) {
        String key = userTokenKey(userId);
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, properties.getAccessTokenTtl());
    }

    private String userTokenKey(Long userId) {
        return "AUTH:USER_TOKENS:" + userId;
    }

    public record RefreshContext(Long userId, String activeRole) {
    }
}
