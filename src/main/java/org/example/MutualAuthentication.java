package org.example;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public class MutualAuthentication {
    public static byte[] generateChallenge() {
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[32];
        random.nextBytes(challenge);
        return challenge;
    }

    public static byte[] signChallenge(PrivateKey privateKey, byte[] challenge) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge);
        return signature.sign();
    }

    public static boolean verifyChallengeSignature(PublicKey publicKey, byte[] challenge, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(challenge);
        return sig.verify(signature);
    }
}