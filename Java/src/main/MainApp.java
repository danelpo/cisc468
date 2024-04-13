package main;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MainApp {
    public static void main(String[] args) {
        try {
        	// Creates file to hold passwords
            String passwordFilePath = "main/passwords.txt";
            UserAuthentication userAuth = new UserAuthentication(passwordFilePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            if (Files.exists(Paths.get(passwordFilePath))) {
            	// Asks if creating new password or signing in with old one
                System.out.println("Log In (1) or Create a New User (2)?");
                String userChoice = reader.readLine().trim();
                if ("1".equals(userChoice)) {
                    if (!userAuth.authenticateUser()) {
                        System.out.println("Authentication failed. Exiting application...");
                        return;
                    }
                } else if ("2".equals(userChoice)) {
                    userAuth.setupNewUser();
                } else {
                    System.out.println("Invalid option selected. Exiting application...");
                    return;
                }
            } else {
                userAuth.setupNewUser();
            }
            
            MessageEncryption encryption = new MessageEncryption();
            ConnectionManager connectionManager = new ConnectionManager(encryption);
            MessageController messageController = null;
            String storagePath = "main/savedMessages.txt";
            Storage storage = new Storage(encryption, storagePath);

            while (true) {
            	// Asking user if broadcasting or discovering.
            	// Discovery cannot find broadcaster, so the discoverer must be done on the Python end
                System.out.println("Welcome to Text Friend Forever!");
                System.out.println("Please choose an option:");
                System.out.println("Broadcast (b), Discover (d), Load Messages (l), Exit (e)?");
                String choice = reader.readLine().trim();

                switch (choice) {
                    case "b":
                        messageController = connectionManager.broadcastConnection();
                        if (messageController != null) {
                            interactWithUser(reader, messageController, storage);
                        }
                        break;
                    case "d":
                        messageController = connectionManager.discoverAndConnect();
                        if (messageController != null) {
                            interactWithUser(reader, messageController, storage);
                        }
                        break;
                    case "l":
                    	// Option meant to load messages saved 
                        System.out.println("Loaded Messages:");
                        List<String> messages = storage.readMessages();
                        for (String message : messages) {
                            System.out.println(message);
                        }
                        System.out.println("Press Enter to continue...");
                        reader.readLine();
                        break;
                    case "e":
                        System.out.println("Exiting application...");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void interactWithUser(BufferedReader reader, MessageController messageController, Storage storage) throws Exception {
    	/*
    	 * Method to interact with the user, asks if sending files and messages or receiving
    	 * 
    	 * @param A reader, a controller and a storage object
    	 * @return void
    	 * 
    	 * */
        while (true) {
        	System.out.println("Type 'f' to send a file, 'rf' to receive a file, 's' to send a message, 'r' to receive a message, or 'q' to return to main menu:");
        	String ui = reader.readLine().trim();
        	switch (ui) {
        	    case "f":
        	        System.out.println("Enter the path of the file to send:");
        	        String filePath = reader.readLine().trim();
        	        messageController.sendFile(filePath);
        	        break;
        	    case "rf":
        	        messageController.receiveFile("main/receivedFiles.bin");
        	        System.out.println("File received and saved.");
        	        break;
                case "s":
                    System.out.println("Please enter the message to be sent:");
                    String messageToSend = reader.readLine().trim();
                    messageController.sendMessage(messageToSend);
                    storage.saveMessage("Sent: " + messageToSend);
                    break;
                case "r":
                    String receivedMessage = messageController.receiveMessage();
                    storage.saveMessage("Received: " + receivedMessage);
                    System.out.println("Received message: " + receivedMessage);
                    break;
                case "q":
                    System.out.println("Returning to main menu...");
                    return;
                default:
                    System.out.println("Incorrect input. Please try again.");
                    break;
            }
        }
    }
}
