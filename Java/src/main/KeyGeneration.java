package main;
import java.security.*;
import javax.crypto.*;

public class KeyGeneration {

    private SecKey secKey;

    private KeyPairGenerator keyPairGen;
    private KeyAgreement keyAgreement;
    private KeyPair keyPair;

    public KeyGeneration() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        this.secKey = keyGen.generateKey();
    }

    public SecKey getSecKey(){
        return this.secKey;
    }


    public void keyRegen() {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGen.init(256, new SecureRandom());
        this.secKey = keyGen.generateKey();
        System.out.println("New secret key generated.");
    }

}