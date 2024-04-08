package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MainApp {

    private KeyGeneration keyGeneration;
    private MessageEncryption messageEncryption;
    private ConnectionManager connectionManager;

    public MainApp() throws NoSuchAlgorithmException {
        this.keyGeneration = new KeyGeneration();
        this.messageEncryption = new MessageEncryption(keyGeneration);
        this.connectionManager = new ConnectionManager();
    }

    public void start() {
        System.out.println("Secure Messaging System");
        System.out.println("1. Broadcast Service");
        System.out.println("2. Discover Services");
        System.out.println("3. Generate Secret Key");
        System.out.println("4. Rotate Secret Key"); 
        System.out.println("5. Exit");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            System.out.println("Enter your choice:");
            while ((line = reader.readLine()) != null && !line.equals("5")) {
                switch (line) {
                    case "1":
                        new Thread(() -> connectionManager.BroadcastConnection()).start();
                        break;
                    case "2":
                        discoverServices();
                        break;
                    case "3":
                        System.out.println("Secret Key: " + Base64.getEncoder().encodeToString(keyGeneration.getSecKey().getEncoded()));
                        break;
                    case "4": 
                        keyGeneration.keyRegen();
                        System.out.println("Secret Key: " + Base64.getEncoder().encodeToString(keyGeneration.getSecretKey().getEncoded()));
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                        break;
                }
                System.out.println("Enter your choice:");
            }
            System.out.println("Exiting...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discoverServices() {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            jmdns.addServiceListener("_broadcaster._tcp.local.", new ServiceListener() {
                @Override
                public void serviceResolved(ServiceEvent event) {
                    System.out.println("Service resolved: " + event.getInfo());
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getName());
                }
            });

            System.out.println("Service discovery running...");
            Thread.sleep(30000); 
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
        	MainApp controller = new MainApp();
            controller.start();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to initialize the system: " + e.getMessage());
        }
    }
}
