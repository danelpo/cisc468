package main;

import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;


public class MessageController {
    private final Socket socket;
    private final MessageEncryption encryption;
    private final Set<Long> TimeStamps = new HashSet<>();

    public MessageController(Socket socket, MessageEncryption encryption) throws IOException {
        this.socket = socket;
        this.encryption = encryption;
    }

    public void sendMessage(String message) throws Exception {
      	/*
    	 * Method to send encrypted message, attaches time stamp to protect against replay attacks
    	 * and a signature for verification and integrity. "!!!!AAAABBBB!!!!" used to parse parts of message.
    	 * 
    	 * @param A message string
    	 * @return void
    	 * 
    	 * */
    	
        String encryptedMessage = encryption.encryptMessage(message);
        long timeStamp = System.currentTimeMillis();
        String signature = encryption.signData(encryptedMessage + timeStamp);
        String combinedMessage = encryptedMessage + "!!!!AAAABBBB!!!!" + timeStamp + "!!!!AAAABBBB!!!!" + signature;
        
        String encodedMessage = Base64.getEncoder().encodeToString(combinedMessage.getBytes());

        // Send the message
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF(encodedMessage);
        out.flush();
    }

    public String receiveMessage() throws Exception {
      	/*
    	 * Method to receive encrypted message, parses encrypted message into message wiht IV, time stamp, and a signature.
    	 * Verifies signature
    	 * @param None
    	 * @return encryption.decryptMessage(encryptedMessageWithIV);
    	 * 
    	 * */
        DataInputStream in = new DataInputStream(socket.getInputStream());
        String encodedMessage = in.readUTF();
        String combinedMessage = new String(Base64.getDecoder().decode(encodedMessage));
        String[] parts = combinedMessage.split("!!!!AAAABBBB!!!!");
        String encryptedMessageWithIV = parts[0];
        long timeStamp = Long.parseLong(parts[1]);
        String signature = parts[2];
        
        long currentTime = System.currentTimeMillis();
        
        // Check for replay attacks
        if ((currentTime - timeStamp) > 5000 || !TimeStamps.add(timeStamp)) {
            throw new SecurityException("Old message, or possible replay attack");
        }

        // Verify signature
        boolean isVerified = encryption.verifySignature(encryptedMessageWithIV + timeStamp, signature);
        if (!isVerified) {
            throw new SecurityException("Signature not verified.");
        }

        // Decrypt the message (decryption method handles IV extraction)
        return encryption.decryptMessage(encryptedMessageWithIV);
    }
    public void sendFile(String filePath) throws Exception {
      	/*
    	 * Method to send encrypted files, 
    	 * @param A string of the filepath
    	 * @return None
    	 * */
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));

        // Encrypt file data
        String encryptedData = encryption.encryptMessage(new String(fileData, StandardCharsets.UTF_8));

        // Send file data
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(encryptedData.length()); // Send the length of the encrypted data
        out.writeUTF(encryptedData); // Send the encrypted data
        out.flush();
    }

    public void receiveFile(String savePath) throws Exception {
      	/*
    	 * Method to receive encrypted files, parses encrypted message into message wiht IV, time stamp, and a signature.
    	 * Verifies signature
    	 * @param String of file that saves file
    	 * @return void
    	 * 
    	 * */
        DataInputStream in = new DataInputStream(socket.getInputStream());

        // Receive file
        int length = in.readInt(); // Read the length of the incoming file data
        if(length > 0) {
            byte[] message = new byte[length];
            in.readFully(message, 0, message.length); // Read the file data
            String encryptedData = new String(message, StandardCharsets.UTF_8);

            // Decrypt data
            byte[] fileData = encryption.decryptMessage(encryptedData).getBytes(StandardCharsets.UTF_8);

            // Write file to file-saving file
            Files.write(Paths.get(savePath), fileData);
        }
    }
}