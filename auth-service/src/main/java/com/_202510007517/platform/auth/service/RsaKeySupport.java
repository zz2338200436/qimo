package com._202510007517.platform.auth.service;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeySupport {

    private RsaKeySupport() {
    }

    public static RSAPublicKey parsePublicKey(String pemText) {
        try {
            byte[] keyBytes = decodePem(pemText, "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("RSA 公钥格式无效", ex);
        }
    }

    public static RSAPrivateKey parsePrivateKey(String pemText) {
        try {
            byte[] keyBytes = decodePem(pemText, "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("RSA 私钥格式无效", ex);
        }
    }

    private static byte[] decodePem(String pemText, String label) {
        String normalized = pemText
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(normalized);
    }
}
