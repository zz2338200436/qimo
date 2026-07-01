package com._202510007517.platform.common.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public final class DevJwtKeyMaterial {

    public static final String KID = "dev";

    private static final byte[] SEED =
            "qimo-platform-development-jwt-rsa-v1".getBytes(StandardCharsets.UTF_8);
    private static final KeyPair KEY_PAIR = generateKeyPair();

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static RSAPrivateKey privateKey() {
        return (RSAPrivateKey) KEY_PAIR.getPrivate();
    }

    private static KeyPair generateKeyPair() {
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            random.setSeed(SEED);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, random);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot create development JWT key pair", ex);
        }
    }

    private DevJwtKeyMaterial() {
    }
}
