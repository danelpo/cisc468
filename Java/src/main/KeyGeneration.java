package main;
import java.security.*;
import javax.crypto.*;

public class KeyGeneration {
    private KeyPairGenerator keyPairGen;
    private KeyAgreement keyAgreement;
    private KeyPair keyPair;
    
    public PublicKey getPublicKey() {
        return this.keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    public KeyGeneration() throws NoSuchAlgorithmException {
        this.keyPairGen = KeyPairGenerator.getInstance("RSA");
        this.keyPairGen.initialize(2048);
        this.keyPair = keyPairGen.generateKeyPair();
        this.keyAgreement = KeyAgreement.getInstance("DH");
    }

    public KeyPair generateKeyPair() {
        return keyPairGen.generateKeyPair();
    }

    public void keyRotate() {
        KeyPair oldPair = this.keyPair;
        this.keyPair = this.keyPairGen.generateKeyPair();
        System.out.println("New Public Key: " + this.keyPair.getPublic().toString());
    }

}