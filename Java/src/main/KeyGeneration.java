package main;
import java.security.*;
import javax.crypto.*;

public class KeyGeneration {

    private SecKey secKey;
    private KeyPair rsaKeyPair;

    private KeyPairGenerator keyPairGen;
    private KeyAgreement keyAgreement;
    private KeyPair keyPair;

    public KeyGeneration() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        this.secKey = keyGen.generateKey();

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        this.rsaKeyPair = keyPairGen.generateKeyPair();
    }

    public SecKey getSecKey(){
        return this.secKey;
    }

    public PrivateKey getPrivateKey() {
        return this.rsaKeyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return this.rsaKeyPair.getPublic();
    }

    public void keyRegen() {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        this.secKey = keyGen.generateKey();
        System.out.println("New secret key generated.");
    }

}