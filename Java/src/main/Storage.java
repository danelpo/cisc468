package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class Storage {
    private final String storagePath;
    private final MessageEncryption encryption;

    public Storage(MessageEncryption encryption, String storagePath) {
        this.encryption = encryption;
        this.storagePath = storagePath;
    }

    public void saveMessage(String message) throws IOException, GeneralSecurityException {
      	/*
    	 * Method to save message to file. 
    	 * 
    	 * @param String of message to save
    	 * @return void
    	 * */
        String encryptedMessage = encryption.encryptMessage(message);
        try (FileOutputStream file = new FileOutputStream(storagePath, true);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file))) {
            writer.write(encryptedMessage);
        }
    }
    
    public List<String> readMessages() throws IOException, GeneralSecurityException {
      	/*
    	 * Method to read messages from files and load
    	 * 
    	 * @param None
    	 * @return messages sent and received
    	 * */
        List<String> messages = new ArrayList<>();
        if (Files.exists(Paths.get(storagePath))) {// Checks if messages file exists
            try (FileInputStream file = new FileInputStream(storagePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(file))) {
                String encryptedMessage;
                while ((encryptedMessage = reader.readLine()) != null) {
                    String message = encryption.decryptMessage(encryptedMessage);
                    messages.add(message);
                }
            }
        }
        return messages;
    }

}