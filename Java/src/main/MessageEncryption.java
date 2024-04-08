package main;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class MessageEncryption {

    private KeyGeneration keyGeneration;

    public MessageEncryption(KeyGeneration keyGeneration) {
        this.keyGeneration = keyGeneration;
    }

    public String encryptWithPublicKey(String data) {
        try {
            PublicKey publicKey = this.keyGeneration.getPublicKey();
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decryptWithPrivateKey(String encryptedData) {
        try {
            PrivateKey privateKey = this.keyGeneration.getPrivateKey();
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encryptMessage(String message, SecretKey aesKey) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(new byte[16]));
            byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            String iv = Base64.getEncoder().encodeToString(cipher.getIV());
            String encryptedContent = Base64.getEncoder().encodeToString(encryptedBytes);
            return String.format("{\"iv\":\"%s\",\"content\":\"%s\"}", iv, encryptedContent);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptMessage(String encryptedMessageJSON, SecretKey aesKey) {
        try {
            String iv = encryptedMessageJSON.split("\"iv\":\"")[1].split("\"")[0];
            String content = encryptedMessageJSON.split("\"content\":\"")[1].split("\"")[0];

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(Base64.getDecoder().decode(iv)));
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public String signData(byte[] data) throws Exception {
        PrivateKey privateKey = keyGeneration.getPrivateKey();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public boolean verifySig(byte[] data, String signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(Base64.getDecoder().decode(signature));
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); 
        return keyGenerator.generateKey();
    }
}