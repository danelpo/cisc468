package main;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MessageController {
	
	public static void main(String[] args) {
		String message = "Hello, world";
		String ip = "127.0.0.1";
		int port = 12345;

        // Sending a message
        sendMessage(message, ip, port);

        // Hashing a message
        String hashedMessage = hashMessage(message);

        // Saving a message
        saveMessage(hashedMessage);

        // Reading messages
        List<String> messages = readMessages();
        if (messages != null) {
            for (String msg : messages) {
                System.out.println("Message: " + msg);
            }
        }
	}
	public static void sendMessage(String message, String ip, int port) {
        try (Socket socket = new Socket(ip, port);
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String hashMessage(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(message.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveMessage(String hashedMessage) {
        String file = "data.txt";
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(hashedMessage + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> readMessages() {
        String file = "data.txt";
        try {
            return Files.readAllLines(Paths.get(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
