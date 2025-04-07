package org.example;

import javax.crypto.KeyAgreement;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class ECDH {
    private static final String ALGORITHM = "EC";
    private static final String CURVE = "secp256r1";
    private KeyPair keyPair;
    private byte[] sharedSecret;
    private SecretKeySpec aesKey;

    public ECDH() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE);
        keyGen.initialize(ecSpec);
        this.keyPair = keyGen.generateKeyPair();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public byte[] generateSharedSecret(PublicKey peerPublicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(peerPublicKey, true);
        this.sharedSecret = keyAgreement.generateSecret();
        return this.sharedSecret;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    public byte[] sessionKey(byte[] sharedSecret) throws NoSuchAlgorithmException {
        byte[] salt = "secure_salt".getBytes();
        PBEKeySpec spec = new PBEKeySpec(
                bytesToHex(sharedSecret).toCharArray(),
                salt,
                100000,
                256
        );
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = skf.generateSecret(spec).getEncoded();
            this.aesKey = new SecretKeySpec(key, "AES");
            return key;
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    public byte[] encrypt(byte[] data) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("Session key not yet generated");
        }
        // Generate random 12-byte nonce
        byte[] nonce = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

        byte[] ciphertext = cipher.doFinal(data);

        // Combine nonce and ciphertext
        byte[] combined = new byte[12 + ciphertext.length];
        System.arraycopy(nonce, 0, combined, 0, 12);
        System.arraycopy(ciphertext, 0, combined, 12, ciphertext.length);

        return combined;
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        if (aesKey == null) {
            throw new IllegalStateException("Session key not yet generated");
        }
        // Extract nonce and ciphertext
        byte[] nonce = Arrays.copyOfRange(encryptedData, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(encryptedData, 12, encryptedData.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        return cipher.doFinal(ciphertext);
    }
}