package main;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;

public class MessageEncryption {
    private final KeyPair rsaKeyPair; 
    private SecretKey aesKey; 
    private KeyPair dhKeyPair;
    private KeyAgreement keyAgreement;

    public MessageEncryption() throws GeneralSecurityException {
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048);
        rsaKeyPair = rsaGen.generateKeyPair();

        initializeDHParameters();

        KeyGenerator aesGen = KeyGenerator.getInstance("AES");
        aesGen.init(256);
        aesKey = aesGen.generateKey();
    }

    private void initializeDHParameters() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
      	/*
    	 * Method to create p and g for DHKE
    	 * 
    	 * @param String of file that saves file
    	 * @return void
    	 * */
        KeyPairGenerator dhGen = KeyPairGenerator.getInstance("DH");
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
        paramGen.init(1024); // 1024 size used to improve efficiency
        AlgorithmParameters params = paramGen.generateParameters();
        DHParameterSpec dhParamSpec;
        try {
            dhParamSpec = params.getParameterSpec(DHParameterSpec.class);
            dhGen.initialize(dhParamSpec);
        } catch (InvalidParameterSpecException e) {
            dhGen.initialize(1024); 
        }
        dhKeyPair = dhGen.generateKeyPair();
        keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(dhKeyPair.getPrivate());
    }

    public String getDHPublicKeyEncoded() {
        return Base64.getEncoder().encodeToString(dhKeyPair.getPublic().getEncoded());
    }

    public void processReceivedPublicKey(String publicKeyEncoded) throws GeneralSecurityException {
      	/*
    	 * Method to process public keys
    	 * 
    	 * This function brought trouble as could not get keyAgreement.doPhase(receivedPublicKey, true); to work
    	 * 
    	 * @param String publicKeyEncoded
    	 * @return void
    	 * */
        System.out.println("Starting to process received public key.");
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyEncoded);

        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("DH");
            PublicKey receivedPublicKey = keyFactory.generatePublic(keySpec);
            
            // part that ran into trouble
            //Would not pass this line, the DH parameters would not align
            keyAgreement.doPhase(receivedPublicKey, true);
            System.out.println("DONE");

            // Get shared secret key
            byte[] sharedSecret = keyAgreement.generateSecret();
            SecretKeySpec newAesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            aesKey = newAesKey;

            System.out.println("Shared secret done");
        } catch (InvalidKeySpecException e) {
            System.out.println("InvalidKeySpecException: " + e.getMessage());
            throw new GeneralSecurityException("Received public key is invalid or incompatible.", e);
        } catch (InvalidKeyException e) {
            System.out.println("InvalidKeyException during phase completion: " + e.getMessage());
            throw new GeneralSecurityException("Invalid public key used.", e);
        }
    }

    public String encryptMessage(String message) throws GeneralSecurityException {
      	/*
    	 * Method to encrypt message
    	 * @param String of message to encrypt
    	 * @return String of encrypted message
    	 * */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        byte[] ivBytes = new byte[16]; // New IV generated every message
        new SecureRandom().nextBytes(ivBytes);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
              
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec); 
        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)); // Final encryption
        
        byte[] ivAndEncryptedMessage = new byte[ivBytes.length + encryptedBytes.length];
        System.arraycopy(ivBytes, 0, ivAndEncryptedMessage, 0, ivBytes.length);
        System.arraycopy(encryptedBytes, 0, ivAndEncryptedMessage, ivBytes.length, encryptedBytes.length);
        
        return Base64.getEncoder().encodeToString(ivAndEncryptedMessage);
    }

    public String decryptMessage(String encryptedData) throws GeneralSecurityException {
      	/*
    	 * Method to decrypt Encrypted data. Extracts IV as first 16 bytes
    	 * @param String encrypted data
    	 * @return String of decrypted message 
    	 * */
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        
        byte[] ivBytes = Arrays.copyOfRange(decodedBytes, 0, 16);
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        byte[] encryptedBytes = Arrays.copyOfRange(decodedBytes, 16, decodedBytes.length);
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes); // Decryption
        
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public String signData(String data) throws GeneralSecurityException {
      	/*
    	 * Method to sign encrypted data.
    	 * 
    	 * @param String of message to be signed
    	 * @return the encoded signature  
    	 * */
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(rsaKeyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    public boolean verifySignature(String data, String signatureStr) throws GeneralSecurityException {
      	/*
    	 * Method to verify signature
    	 * @param String of the data and the stringed signature
    	 * @return the verification
    	 * */
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(rsaKeyPair.getPublic());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(signatureStr));
    }
    public SecretKey getAesKey() {
        return aesKey;
    }

    public PublicKey getPublicKey() {
        return rsaKeyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return rsaKeyPair.getPrivate();
    }
}