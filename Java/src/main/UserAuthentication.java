package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;

public class UserAuthentication {
    private final String passwordFilePath;

    public UserAuthentication(String passwordFilePath) {
        this.passwordFilePath = passwordFilePath;
    }

    public void setupNewUser() throws Exception {
      	/*
    	 * Method to set up new user if needed. 
    	 * Creates new password and saves it
    	 * 
    	 * @param None
    	 * @return void
    	 * */
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("No existing user found. Please set up a new password.");
        System.out.print("Enter new password: ");
        String newPassword = reader.readLine();
        savePassword(newPassword);
    }

    private void savePassword(String password) throws Exception {
      	/*
    	 * Method to save password to file. 
    	 * 
    	 * @param String of password to save
    	 * @return void
    	 * */
        String hashedPassword = hashPassword(password);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(passwordFilePath))) {
            writer.write(hashedPassword);
        }
    }

    public boolean authenticateUser() throws Exception {
      	/*
    	 * Method to check user password against password file
    	 * 
    	 * @param None
    	 * @return validation of password
    	 * */
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Enter your password: ");
        String inputPassword = reader.readLine();
        String hashedInput = hashPassword(inputPassword);

        String storedHash = Files.readAllLines(Paths.get(passwordFilePath)).get(0);
        return hashedInput.equals(storedHash);
    }

    private String hashPassword(String password) throws Exception {
      	/*
    	 * Method to hash password for saving
    	 * 
    	 * @param password
    	 * @return hashed password
    	 * */
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash); // Encode hash as base64 string
    }
}