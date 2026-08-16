package com.chronos.infrastructure.device;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DeviceCredentialHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private final SecureRandom random = new SecureRandom();

    public String newSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String newSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public String hash(String secret, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS);
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            spec.clearPassword();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash device credential", e);
        }
    }

    public boolean matches(String secret, String salt, String expectedHash) {
        if (secret == null || salt == null || expectedHash == null) return false;
        return MessageDigest.isEqual(hash(secret, salt).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                expectedHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
