package main;

import java.io.*;
import javax.jmdns.ServiceInfo;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.Optional;

public class MessageController {

    public static void main(String[] args) {

        Scanner scan = new Scanner(System.in);
        System.out.println("Enter your message:");
        String message = scan.nextLine();

        byte[] key = MessageEncryption.generateRandomKey();
        byte[] iv = MessageEncryption.generateRandomIV(); 
        byte[] encryptedMessage = MessageEncryption.encryptMessage(message, key, iv);

        if (encryptedMessage == null) {
            System.err.println("Error encrypting the message.");
            return;
        }
        Optional<ServiceInfo> optionalServiceInfo = PeerDiscovery.getFirstDiscoveredService();
        if (!optionalServiceInfo.isPresent()) {
            System.err.println("No services discovered.");
            scan.close();
            return;
        }

        ServiceInfo serviceInfo = optionalServiceInfo.get();
        String ip = serviceInfo.getHostAddresses()[0];
        int port = serviceInfo.getPort();

        String encryptedMessageBase64 = Base64.getEncoder().encodeToString(encryptedMessage);


        sendMessage(encryptedMessageBase64, ip, port);

        String hashedMessage = hashMessage(message);
        if (hashedMessage != null) {
            saveMessage(hashedMessage);
        }

        List<String> messages = readMessages();
        if (messages != null) {
            for (String msg : messages) {
                System.out.println("Saved Message: " + msg);
            }
        }

        scan.close();
    }

    public static void sendMessage(String message, String ip, int port) {
        try (Socket socket = new Socket(ip, port);
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(message.getBytes());
        } catch (IOException e) {
            System.err.println("Error sending the message: " + e.getMessage());
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
            System.err.println("Error hashing the message: " + e.getMessage());
            return null;
        }
    }

    public static void saveMessage(String hashedMessage) {
        String file = "data.txt";
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(hashedMessage + "\n");
        } catch (IOException e) {
            System.err.println("Error saving the message: " + e.getMessage());
        }
    }

    public static List<String> readMessages() {
        String file = "data.txt";
        try {
            return Files.readAllLines(Paths.get(file));
        } catch (IOException e) {
            System.err.println("Error reading messages: " + e.getMessage());
            return null;
        }
    }
}
