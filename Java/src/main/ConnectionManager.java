package main;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private List<Socket> clients = new ArrayList<>();

    public static void main(String[] args) {
        ConnectionManager manager = new ConnectionManager();
        manager.BroadcastConnection();
    }

    public void BroadcastConnection() {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            String type = "_broadcaster._tcp.local.";
            String name = "ExampleService._example._tcp.local.";
            int port = 3000;
            
            ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, "path=index.html");
            jmdns.registerService(serviceInfo);
            System.out.println("Service published and awaiting connections...");

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                    clients.add(clientSocket);
                    new Thread(() -> handleClientConnection(clientSocket)).start();
                }
            } finally {
                jmdns.unregisterService(serviceInfo);
                jmdns.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleClientConnection(Socket clientSocket) {
    	
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);
            }
        } catch (IOException e) {
            System.out.println("Connection with " + clientSocket.getRemoteSocketAddress() + " closed.");
        } finally {
            clients.remove(clientSocket);
            try {
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }
}